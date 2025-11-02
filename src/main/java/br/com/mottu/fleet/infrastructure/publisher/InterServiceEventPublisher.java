package br.com.mottu.fleet.infrastructure.publisher;

import br.com.mottu.fleet.application.dto.integration.InterServiceMessage;
import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class InterServiceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InterServiceEventPublisher.class);
    private static final String QUEUE_NAME = "funcionario-criado-queue";

    private final ObjectMapper objectMapper;
    private final ServiceBusSenderClient senderClient;

    public InterServiceEventPublisher(ObjectMapper objectMapper,
                                     @Value("${spring.jms.servicebus.connection-string}") String connectionString) {
        this.objectMapper = objectMapper;
        this.senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(QUEUE_NAME)
                .buildClient();
    }


    /**
     * Publica um evento de ciclo de vida (de qualquer entidade) na fila de sincronização.
     *
     * @param payload O DTO de estado (ex: FuncionarioSyncPayload, PateoSyncPayload)
     * @param eventType O tipo de evento (ex: "FUNCIONARIO_CRIADO").
     */
    @Async
    public void publishEvent(Object payload, String eventType) { // <-- Método agora é genérico
        log.info("Publicando evento de sincronização: {} para a fila '{}'", eventType, QUEUE_NAME);
        try {
            InterServiceMessage messagePayload = new InterServiceMessage(eventType, payload);
            
            String jsonPayload = objectMapper.writeValueAsString(messagePayload);
            
            ServiceBusMessage message = new ServiceBusMessage(jsonPayload);
            message.setContentType("application/json");
            senderClient.sendMessage(message);
            
            log.info("Mensagem de {} enviada com sucesso.", eventType);

        } catch (Exception e) {
            log.error("Falha ao serializar ou enviar mensagem de sincronização ({}): {}", eventType, e.getMessage(), e);
        }
    }
}
