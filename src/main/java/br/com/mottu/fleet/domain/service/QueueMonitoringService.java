package br.com.mottu.fleet.domain.service;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import com.azure.messaging.servicebus.administration.models.QueueRuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class QueueMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(QueueMonitoringService.class);
    private final ServiceBusAdministrationClient adminClient;

    public QueueMonitoringService(@Value("${spring.jms.servicebus.connection-string}") String connectionString) {
        // Usa a mesma connection string dos listeners para criar um cliente de ADMINISTRAÇÃO
        this.adminClient = new ServiceBusAdministrationClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    }

    /**
     * Busca as estatísticas em tempo real das filas de notificação no Azure Service Bus.
     * @return Um Mapa onde a chave é o nome da fila e o valor são suas propriedades.
     */
    public Map<String, QueueRuntimeProperties> getQueueStats() {
        Map<String, QueueRuntimeProperties> stats = new HashMap<>();

        // Lista das filas que queremos monitorar
        String[] queuesToMonitor = {"whatsapp-failures-queue", "email-failures-queue"};

        for (String queueName : queuesToMonitor) {
            try {
                // Busca as propriedades da fila (que contêm as contagens de mensagens)
                QueueRuntimeProperties properties = adminClient.getQueueRuntimeProperties(queueName);
                stats.put(queueName, properties);
            } catch (Exception e) {
                // Se a fila não existir ou houver um erro, apenas loga e continua
                log.warn("Não foi possível buscar estatísticas para a fila: {}. Erro: {}", queueName, e.getMessage());
            }
        }
        return stats;
    }
}