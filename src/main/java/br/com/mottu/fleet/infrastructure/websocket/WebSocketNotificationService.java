package br.com.mottu.fleet.infrastructure.websocket;

import br.com.mottu.fleet.application.dto.websocket.NotificationStatusPayload;
import br.com.mottu.fleet.domain.entity.Funcionario;

import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

    private final AdminNotificationSocketHandler handler;

    public WebSocketNotificationService(AdminNotificationSocketHandler handler) {
        this.handler = handler;
    }

    public void sendNotificationStatus(Funcionario funcionario, String status, String message, String fallbackMagicLink) {
        if (funcionario == null || funcionario.getPateo() == null) {
            return; // Não podemos enviar se não soubermos para qual pátio.
        }

        NotificationStatusPayload payload = new NotificationStatusPayload(
            "NOTIFICATION_STATUS_UPDATE",
            funcionario.getPateo().getId(),
            funcionario.getId(),
            status,
            message,
            fallbackMagicLink
        );

        handler.sendMessageToPateo(funcionario.getPateo().getId(), payload);
    }
}