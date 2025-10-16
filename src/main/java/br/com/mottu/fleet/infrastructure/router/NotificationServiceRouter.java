package br.com.mottu.fleet.infrastructure.router;

import br.com.mottu.fleet.domain.service.NotificationService;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NotificationServiceRouter {

    private final Map<String, NotificationService> serviceMap;

    /**
     * Lista de todos os beans que implementam a NotificationService
     */
    public NotificationServiceRouter(List<NotificationService> services) {
        this.serviceMap = services.stream()
                .collect(Collectors.toMap(
                    service -> service.getClass().getSimpleName().substring(0, 1).toLowerCase() + service.getClass().getSimpleName().substring(1),
                    Function.identity()
                ));
    }
    
    /**
     * Retorna uma implementação específica do NotificationService
     * @param qualifier O nome do bean
     * @return A instância do serviço correspondente.
     */
    public NotificationService getService(String qualifier) {
        NotificationService service = serviceMap.get(qualifier);
        if (service == null) {
            service = serviceMap.get(qualifier.replace("Impl", ""));
        }
        if (service == null) {
            throw new IllegalArgumentException("Nenhum NotificationService encontrado para o qualificador: " + qualifier);
        }
        return service;
    }
}