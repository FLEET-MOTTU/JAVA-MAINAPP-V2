package br.com.mottu.fleet.infrastructure.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.service.NotificationService;
import br.com.mottu.fleet.infrastructure.websocket.WebSocketNotificationService;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementação de "último recurso" do NotificationService.
 * Acionada quando todos os outros métodos de notificação (WhatsApp, E-mail) falham.
 * Sua responsabilidade é registrar a falha de forma persistente (log) e
 * notificar o frontend via WebSocket para que o admin possa tomar uma ação manual.
 */
@Service("lastResortNotificationServiceImpl")
public class LastResortNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LastResortNotificationServiceImpl.class);

    private final WebSocketNotificationService webSocketService;

    public LastResortNotificationServiceImpl(WebSocketNotificationService webSocketService) {
        this.webSocketService = webSocketService;
    }


    /**
     * Loga um erro crítico e envia uma notificação via WebSocket para o
     * admin do pátio, incluindo o Magic Link para ação manual.
     *
     * @param funcionario O funcionário que não pôde ser contatado.
     * @param fallbackMagicLink A URL do Magic Link que falhou em ser entregue.
     */
    @Override
    public void enviarMagicLink(Funcionario funcionario, String fallbackMagicLink) {
        
        String msg = String.format(
            "FALHA FINAL: WhatsApp e E-mail falharam para %s. Ação manual necessária.",
            funcionario.getNome()
        );
        
        log.error("Acionando fallback final para o funcionário ID: {}. Link de resgate: {}",
            funcionario.getId(), fallbackMagicLink);

        webSocketService.sendNotificationStatus(
            funcionario,
            "FINAL_FAILURE",
            msg,
            fallbackMagicLink // Envia o link para o admin copiar e colar
        );
    }
}