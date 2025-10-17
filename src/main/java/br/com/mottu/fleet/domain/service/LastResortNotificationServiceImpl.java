package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.infrastructure.websocket.WebSocketNotificationService;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service("lastResortNotificationServiceImpl")
public class LastResortNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LastResortNotificationServiceImpl.class);
    private final WebSocketNotificationService webSocketService;

    public LastResortNotificationServiceImpl(WebSocketNotificationService webSocketService) {
        this.webSocketService = webSocketService;
    }

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
            fallbackMagicLink
        );
    }
}