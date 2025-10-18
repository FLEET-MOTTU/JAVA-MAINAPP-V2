package br.com.mottu.fleet.infrastructure.publisher;

import br.com.mottu.fleet.application.dto.integration.FuncionarioSyncPayload;
import br.com.mottu.fleet.application.dto.integration.QueueMessagePayload;
import br.com.mottu.fleet.domain.entity.Funcionario;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FuncionarioEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FuncionarioEventPublisher.class);
    private static final String QUEUE_NAME = "funcionario-criado-queue";

    private final ObjectMapper objectMapper;
    private final ServiceBusSenderClient senderClient;

    public FuncionarioEventPublisher(ObjectMapper objectMapper,
                                     @Value("${spring.jms.servicebus.connection-string}") String connectionString) {
        this.objectMapper = objectMapper;
        // Criamos um "enviador" nativo do Azure SDK
        this.senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(QUEUE_NAME)
                .buildClient();
    }

    /**
     * Publica um evento de ciclo de vida do funcionário na fila de sincronização.
     * Este método é assíncrono (@Async) para não bloquear a thread principal da API.
     *
     * @param funcionario O funcionário que sofreu a alteração.
     * @param eventType O tipo de evento (ex: "FUNCIONARIO_CRIADO").
     */
    @Async
    public void publishFuncionarioEvent(Funcionario funcionario, String eventType) {
        log.info("Publicando evento de sincronização: {} para o funcionário ID: {}", eventType, funcionario.getId());
        try {
            FuncionarioSyncPayload data = new FuncionarioSyncPayload(funcionario);
            QueueMessagePayload payload = new QueueMessagePayload(eventType, data);
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            // Usamos o SDK nativo para criar e enviar a mensagem
            ServiceBusMessage message = new ServiceBusMessage(jsonPayload);
            message.setContentType("application/json");

            senderClient.sendMessage(message);
            
            log.info("Mensagem de {} enviada para a fila '{}'", eventType, QUEUE_NAME);
        } catch (Exception e) {
            log.error("Falha ao serializar ou enviar mensagem de sincronização para o funcionário ID: {}", funcionario.getId(), e);
        }
    }
}