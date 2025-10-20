package br.com.mottu.fleet.infrastructure.publisher;

import br.com.mottu.fleet.application.dto.integration.FuncionarioSyncPayload;
import br.com.mottu.fleet.application.dto.integration.QueueMessagePayload;
import br.com.mottu.fleet.domain.entity.Funcionario;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Publisher responsável por enviar eventos de ciclo de vida do funcionário
 * para a fila de sincronização do Azure Service Bus.
 * Desacopla o domínio de negócio da infraestrutura de mensageria.
 */
@Service
public class FuncionarioEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FuncionarioEventPublisher.class);

    private static final String QUEUE_NAME = "funcionario-criado-queue";
    private final ObjectMapper objectMapper;
    private final ServiceBusSenderClient senderClient;

    public FuncionarioEventPublisher(ObjectMapper objectMapper,
                                     @Value("${spring.jms.servicebus.connection-string}") String connectionString) {
        this.objectMapper = objectMapper;

        this.senderClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .queueName(QUEUE_NAME)
                .buildClient();
    }


    /**
     * Publica um evento de ciclo de vida do funcionário (CRIADO, ATUALIZADO, DESATIVADO)
     * na fila de sincronização para ser consumido por outros serviços (API de C#).
     * Método executado de forma assíncrona (@Async) para não bloquear a thread
     * principal do JAVA.
     *
     * @param funcionario O objeto da entidade Funcionario com seu estado mais recente.
     * @param eventType Uma String que define o tipo de evento (ex: "FUNCIONARIO_CRIADO").
     */
    @Async
    public void publishFuncionarioEvent(Funcionario funcionario, String eventType) {
        log.info("Publicando evento de sincronização: {} para o funcionário ID: {}", eventType, funcionario.getId());
        try {
            // 1. Converte a entidade JPA no "contrato" (DTO de estado)
            FuncionarioSyncPayload data = new FuncionarioSyncPayload(funcionario);
            
            // 2. Cria o "envelope" genérico da mensagem
            QueueMessagePayload payload = new QueueMessagePayload(eventType, data);
            
            // 3. Serializa o envelope completo para uma string JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            // 4. Usa o SDK nativo da Azure para criar a mensagem
            ServiceBusMessage message = new ServiceBusMessage(jsonPayload);
            message.setContentType("application/json");

            // 5. Envia a mensagem para a fila
            senderClient.sendMessage(message);
            
            log.info("Mensagem de {} enviada para a fila '{}'", eventType, QUEUE_NAME);

        } catch (Exception e) {
            log.error("Falha ao serializar ou enviar mensagem de sincronização para o funcionário ID: {}", funcionario.getId(), e);
        }
    }
    
}