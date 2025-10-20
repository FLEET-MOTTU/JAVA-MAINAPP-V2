package br.com.mottu.fleet.infrastructure.router;

import br.com.mottu.fleet.domain.service.NotificationService;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Componente de infraestrutura que cataloga todas as implementações da interface NotificationService
 * e permite que outros serviços selecionem uma implementação específica em tempo de execução
 * usando um qualificador (o nome da classe do bean).
 */
@Component
public class NotificationServiceRouter {

    // Um Map onde a chave é o nome do bean
    private final Map<String, NotificationService> serviceMap;

    /**
     * Construtor que utiliza a injeção de lista do Spring.
     * O Spring injeta automaticamente todos os beans que implementam a interface NotificationService.
     * @param services A lista de todas as implementações de NotificationService encontradas no contexto.
     */
    public NotificationServiceRouter(List<NotificationService> services) {
        this.serviceMap = services.stream()
                .collect(Collectors.toMap(
                    service -> service.getClass().getSimpleName().substring(0, 1).toLowerCase() + service.getClass().getSimpleName().substring(1),
                    Function.identity()
                ));
    }

    
    /**
     * Retorna uma implementação específica do NotificationService com base no qualifier.
     *
     * @param qualifier O nome do bean (ex: "sendGridEmailNotificationServiceImpl").
     * @return A instância do serviço correspondente.
     * @throws IllegalArgumentException Se nenhum serviço for encontrado para o qualificador.
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