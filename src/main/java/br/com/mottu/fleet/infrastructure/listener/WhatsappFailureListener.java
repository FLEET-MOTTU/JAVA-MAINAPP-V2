package br.com.mottu.fleet.infrastructure.listener;

import br.com.mottu.fleet.domain.service.NotificationProcessingService;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.DeadLetterOptions;

import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Consumer que ouve a fila 'whatsapp-failures-queue' no Azure Service Bus.
 * Responsável por receber o webhook de falha do Twilio (enviado pelo TwilioWebhookController),
 * parsear a mensagem e delegar o processamento para o serviço de fallback.
 */
@Component
public class WhatsappFailureListener {

    private static final Logger log = LoggerFactory.getLogger(WhatsappFailureListener.class);

    private final NotificationProcessingService notificationProcessingService;
    private final ServiceBusProcessorClient processorClient;

    public WhatsappFailureListener(NotificationProcessingService notificationProcessingService,
                                  @Value("${AZURE_SERVICEBUS_CONNECTION_STRING}") String connectionString) {
        this.notificationProcessingService = notificationProcessingService;

        this.processorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName("whatsapp-failures-queue")
                .disableAutoComplete()
                .processMessage(this::handleMessage)
                .processError(this::handleError)
                .buildProcessorClient();
    }


    @PostConstruct
    public void start() {
        log.info("Iniciando listener do Azure Service Bus para a fila 'whatsapp-failures-queue'...");
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
        log.info("Mensagem recebida da fila 'whatsapp-failures-queue': {}", body);

        try {
            // 1. PARSE DO PAYLOAD
            // O webhook do Twilio envia os dados como 'application/x-www-form-urlencoded'
            Map<String, String> params = parseUrlEncodedString(body);

            String messageSid = params.get("MessageSid");
            String messageStatus = params.get("MessageStatus");
            
            // 2. DELEGAÇÃO PARA O SERVIÇO DE PROCESSAMENTO
            if (messageSid != null && messageStatus != null) {
                log.info("Processando falha para MessageSID: {} com status: {}", messageSid, messageStatus);
                notificationProcessingService.processarStatusDaMensagem(messageSid, messageStatus);
            } else {
                log.warn("Mensagem da fila não continha MessageSid ou MessageStatus. Payload: {}", body);
            }

            // 3. CONFIRMAÇÃO DO PROCESSAMENTO
            context.complete();

        } catch (Exception e) {
            log.error("Erro ao processar mensagem da fila do Service Bus.", e);

            DeadLetterOptions options = new DeadLetterOptions()
                    .setDeadLetterReason("Erro ao processar mensagem")
                    .setDeadLetterErrorDescription(e.getMessage());

            try {
                context.deadLetter(options);
            } catch (Exception ex) {
                log.error("Falha ao enviar mensagem para a Dead Letter Queue: {}", ex.getMessage(), ex);
            }
        }
    }


    /**
     * Handler de erro do próprio listener do Service Bus (ex: falha de conexão).
     * @param context O contexto do erro.
     */
    private void handleError(ServiceBusErrorContext context) {
        log.error("Erro no listener do Azure Service Bus. Entidade: {}, Erro: {}",
                context.getEntityPath(),
                context.getException().getMessage());
    }


    /**
     * Método auxiliar para converter o payload do Twilio (form-urlencoded) em um Map.
     * @param urlEncoded A string de payload.
     * @return Um Map com os pares de chave/valor.
     */
    private Map<String, String> parseUrlEncodedString(String urlEncoded) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        String[] pairs = urlEncoded.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                map.put(key, value);
            }
        }
        return map;
    }
    
}
