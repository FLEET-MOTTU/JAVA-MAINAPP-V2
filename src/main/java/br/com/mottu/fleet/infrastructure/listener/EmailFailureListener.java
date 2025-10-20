package br.com.mottu.fleet.infrastructure.listener;

import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.infrastructure.router.NotificationServiceRouter;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Consumer que ouve a fila 'email-failures-queue' no Azure Service Bus.
 * Responsável por processar falhas reportadas pelo SendGrid, encontrar o funcionário
 * correspondente e acionar o último serviço de fallback (log).
 */
@Component
public class EmailFailureListener {

    private static final Logger log = LoggerFactory.getLogger(EmailFailureListener.class);

    private final ServiceBusProcessorClient processorClient;
    private final ObjectMapper objectMapper;
    private final FuncionarioRepository funcionarioRepository;
    private final NotificationServiceRouter notificationRouter;
    private final TokenAcessoRepository tokenAcessoRepository;
    private final String baseUrl;

    public EmailFailureListener(ObjectMapper objectMapper, 
                                FuncionarioRepository funcionarioRepository, 
                                NotificationServiceRouter notificationRouter,
                                TokenAcessoRepository tokenAcessoRepository,
                                @Value("${application.base-url}") String baseUrl,
                                @Value("${spring.jms.servicebus.connection-string}") String connectionString) {
        this.objectMapper = objectMapper;
        this.funcionarioRepository = funcionarioRepository;
        this.notificationRouter = notificationRouter;
        this.tokenAcessoRepository = tokenAcessoRepository;
        this.baseUrl = baseUrl;

        this.processorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName("email-failures-queue")
                .disableAutoComplete()
                .processMessage(this::handleMessage)
                .processError(this::handleError)
                .buildProcessorClient();
    }


    @PostConstruct
    public void start() {
        log.info("Iniciando listener do Azure Service Bus para a fila 'email-failures-queue'...");
        processorClient.start();
    }


    @PreDestroy
    public void stop() {
        log.info("Encerrando listener do Azure Service Bus...");
        processorClient.close();
    }


    /**
     * Handler principal, chamado automaticamente para cada mensagem recebida na fila.
     * @param context O contexto da mensagem recebida.
     */
    private void handleMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        String body = message.getBody().toString();
        log.warn("MENSAGEM DE FALHA DE E-MAIL RECEBIDA! Conteúdo: {}", body);

        try {
            List<Map<String, Object>> events;
            String trimmedBody = body.trim();

            // 1. PARSE DO PAYLOAD
            // O SendGrid pode enviar um único evento (objeto JSON) ou um lote (array JSON) dependendo do erro
            if (trimmedBody.startsWith("[")) {
                events = objectMapper.readValue(trimmedBody, new TypeReference<>() {});
            } else {
                Map<String, Object> singleEvent = objectMapper.readValue(trimmedBody, new TypeReference<>() {});
                events = Collections.singletonList(singleEvent);
            }

            // 2. PROCESSAMENTO DE CADA EVENTO DE FALHA
            for (Map<String, Object> event : events) {
                String email = (String) event.get("email");
                String reason = (String) event.get("reason");
                String eventType = (String) event.get("event");

                log.error("Falha ao enviar e-mail para [{}]. Evento: [{}]. Motivo: [{}]", email, eventType, reason);
                
                // 3. LÓGICA DE FALLBACK
                // Tenta encontrar o funcionário pelo e-mail que falhou
                funcionarioRepository.findByEmail(email).ifPresentOrElse(
                    funcionario -> {
                        // Busca o último Magic Link válido para o funcionário
                        tokenAcessoRepository.findFirstByFuncionarioAndUsadoIsFalseAndExpiraEmAfterOrderByCriadoEmDesc(funcionario, Instant.now())
                            .ifPresentOrElse(
                                // Se encontrou um link válido, aciona o último recurso com o link
                                token -> {
                                    String magicLinkReal = baseUrl + "/auth/validar-token?valor=" + token.getToken();
                                    log.info("Funcionário {} encontrado. Acionando serviço de último recurso com o link real.", funcionario.getNome());
                                    notificationRouter.getService("lastResortNotificationServiceImpl")
                                        .enviarMagicLink(funcionario, magicLinkReal);
                                },
                                () -> {
                                    // Se não encontrou link válido, aciona o último recurso com uma msg de erro
                                    log.error("Funcionário {} encontrado, mas NENHUM Magic Link válido foi encontrado para ele no banco.", funcionario.getNome());
                                    notificationRouter.getService("lastResortNotificationServiceImpl")
                                        .enviarMagicLink(funcionario, "Falha geral na entrega e nenhum link válido foi encontrado.");
                                }
                            );
                    },
                    () -> log.warn("Recebida falha de e-mail para um endereço ({}) que não corresponde a nenhum funcionário no banco de dados.", email)
                );
            }

            // 4. CONFIRMAÇÃO DO PROCESSAMENTO
            // Informa à fila que a mensagem foi processada com sucesso e pode ser removida.
            context.complete();

        } catch (Exception e) {
            log.error("Erro CRÍTICO ao processar mensagem da fila de falhas de e-mail. Movendo para a Dead Letter Queue.", e);
            DeadLetterOptions options = new DeadLetterOptions()
                .setDeadLetterReason("Erro no processamento do listener")
                .setDeadLetterErrorDescription(e.getMessage());
            try {
                context.deadLetter(options);
            } catch (Exception dlqEx) {
                log.error("FALHA AO ENVIAR PARA A DEAD LETTER QUEUE. A mensagem pode ser perdida.", dlqEx);
            }
        }
    }


    /**
     * Handler de erro do próprio listener do Service Bus (ex: falha de conexão).
     * @param context O contexto do erro.
     */
    private void handleError(ServiceBusErrorContext context) {
        log.error("Erro no listener da fila de e-mails. Entidade: {}, Erro: {}",
                context.getEntityPath(),
                context.getException().getMessage());
    }

}