package br.com.mottu.fleet.infrastructure.service;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.QueueRuntimeProperties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Serviço de infraestrutura para o painel do Super Admin.
 * Responsável por se conectar ao Azure Service Bus e obter estatísticas
 * em tempo real sobre o estado das filas de notificação.
 */
@Service
public class QueueMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(QueueMonitoringService.class);

    private final ServiceBusAdministrationClient adminClient;

    public QueueMonitoringService(@Value("${spring.jms.servicebus.connection-string}") String connectionString) {
        // Usa a mesma connection string dos listeners para criar um cliente de admin
        this.adminClient = new ServiceBusAdministrationClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    }


    /**
     * Busca as estatísticas em tempo real das filas de notificação no Azure Service Bus.
     * @return Um Mapa onde a chave é o nome da fila e o valor são suas propriedades (QueueRuntimeProperties).
     */
    public Map<String, QueueRuntimeProperties> getQueueStats() {
        Map<String, QueueRuntimeProperties> stats = new HashMap<>();

        // Lista das filas para monitorar (hardcoded)
        String[] queuesToMonitor = {"whatsapp-failures-queue", "email-failures-queue", "funcionario-criado-queue"};

        for (String queueName : queuesToMonitor) {
            try {
                QueueRuntimeProperties properties = adminClient.getQueueRuntimeProperties(queueName);
                stats.put(queueName, properties);
            } catch (Exception e) {
                log.warn("Não foi possível buscar estatísticas para a fila: {}. Erro: {}", queueName, e.getMessage());
            }
        }
        return stats;
    }
}