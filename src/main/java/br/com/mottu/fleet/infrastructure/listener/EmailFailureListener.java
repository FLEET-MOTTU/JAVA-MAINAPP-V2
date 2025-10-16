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

private void handleMessage(ServiceBusReceivedMessageContext context) {
    ServiceBusReceivedMessage message = context.getMessage();
    String body = message.getBody().toString();
    log.warn("MENSAGEM DE FALHA DE E-MAIL RECEBIDA! Conteúdo: {}", body);

    try {
        List<Map<String, Object>> events;
        String trimmedBody = body.trim();

        // Verifica se a mensagem é um array (começa com '[') ou um objeto único (começa com '{')
        if (trimmedBody.startsWith("[")) {
            // Se for um array (como no botão de teste do SendGrid), parseia como uma lista.
            events = objectMapper.readValue(trimmedBody, new TypeReference<>() {});
        } else {
            // Se for um objeto único (como em uma falha real), parseia como um único mapa...
            Map<String, Object> singleEvent = objectMapper.readValue(trimmedBody, new TypeReference<>() {});
            // ...e o coloca dentro de uma lista para que o código do loop funcione para ambos os casos.
            events = Collections.singletonList(singleEvent);
        }

        // 2. PROCESSAMENTO DE CADA EVENTO DE FALHA
        for (Map<String, Object> event : events) {
            String email = (String) event.get("email");
            String reason = (String) event.get("reason");
            String eventType = (String) event.get("event");

            log.error("Falha ao enviar e-mail para [{}]. Evento: [{}]. Motivo: [{}]", email, eventType, reason);
            
            // 3. LÓGICA REAL DE BUSCA NO BANCO DE DADOS
            funcionarioRepository.findByEmail(email).ifPresentOrElse(
                // Ação se o funcionário for encontrado com base no e-mail
                funcionario -> {
                    // Agora, busca o último token válido para este funcionário
                    tokenAcessoRepository.findFirstByFuncionarioAndUsadoIsFalseAndExpiraEmAfterOrderByCriadoEmDesc(funcionario, Instant.now())
                        .ifPresentOrElse(
                            // Se um token válido for encontrado...
                            token -> {
                                String magicLinkReal = baseUrl + "/auth/validar-token?valor=" + token.getToken();
                                log.info("Funcionário {} encontrado. Acionando serviço de último recurso com o link real.", funcionario.getNome());
                                notificationRouter.getService("lastResortNotificationServiceImpl")
                                    .enviarMagicLink(funcionario, magicLinkReal);
                            },
                            // Se o funcionário foi encontrado, mas não há um token válido...
                            () -> {
                                log.error("Funcionário {} encontrado, mas NENHUM Magic Link válido foi encontrado para ele no banco.", funcionario.getNome());
                                notificationRouter.getService("lastResortNotificationServiceImpl")
                                    .enviarMagicLink(funcionario, "Falha geral na entrega e nenhum link válido foi encontrado.");
                            }
                        );
                },
                // Ação se nenhum funcionário for encontrado com aquele e-mail
                () -> log.warn("Recebida falha de e-mail para um endereço ({}) que não corresponde a nenhum funcionário no banco de dados.", email)
            );
        }

        // 4. CONFIRMAÇÃO DO PROCESSAMENTO
        // Se o código chegou até aqui sem erros, a mensagem foi processada com sucesso.
        context.complete();

    } catch (Exception e) {
        // 5. TRATAMENTO DE ERROS INESPERADOS
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

    private void handleError(ServiceBusErrorContext context) {
        log.error("Erro no listener da fila de e-mails. Entidade: {}, Erro: {}",
                context.getEntityPath(),
                context.getException().getMessage());
    }
}