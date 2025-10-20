package br.com.mottu.fleet.infrastructure.websocket;

import br.com.mottu.fleet.application.dto.websocket.NotificationStatusPayload;
import br.com.mottu.fleet.domain.entity.Funcionario;

import org.springframework.stereotype.Service;


/**
 * Serviço para simplificar o envio de notificações via WebSocket.
 * Desacopla a lógica de negócio (listeners) dos detalhes de implementação
 * do WebSocketHandler (construção do payload).
 */
@Service
public class WebSocketNotificationService {

    private final AdminNotificationSocketHandler handler;

    public WebSocketNotificationService(AdminNotificationSocketHandler handler) {
        this.handler = handler;
    }


    /**
     * Constrói o payload de notificação de status e envia para o pátio correto
     * pelo WebSocket handler.
     *
     * @param funcionario O funcionário que é o sujeito da notificação.
     * @param status Uma string que representa o novo status (ex: "WHATSAPP_FAILED").
     * @param message Uma mensagem amigável para exibição no frontend.
     * @param fallbackMagicLink Opcional. A URL do Magic Link para o admin usar manualmente em caso de falha total.
     */
    public void sendNotificationStatus(Funcionario funcionario, String status, String message, String fallbackMagicLink) {

        if (funcionario == null || funcionario.getPateo() == null) {
            return; // Validação de segurança: não enviar se não souber para qual pátio.
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