package br.com.mottu.fleet.application.dto.websocket;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;


/**
 * DTO (Payload) para as mensagens de notificação em tempo real enviadas via WebSocket.
 * Informa ao frontend o status do envio de notificações (WhatsApp, E-mail e Fallback.).
 */
@Schema(description = "Payload para mensagens de notificação em tempo real via WebSocket.")
public record NotificationStatusPayload(
    
    @Schema(description = "Tipo do evento para o frontend rotear a ação", example = "NOTIFICATION_STATUS_UPDATE")
    String type,
    
    @Schema(description = "ID do Pátio ao qual o funcionário pertence")
    UUID pateoId,
    
    @Schema(description = "ID do Funcionário que é o alvo da notificação")
    UUID funcionarioId,
    
    @Schema(description = "Código de status da notificação", example = "WHATSAPP_FAILED")
    String status,
    
    @Schema(description = "Mensagem amigável para exibição na UI", example = "Falha ao enviar WhatsApp. Tentando e-mail...")
    String message,
    
    @Schema(description = "Opcional. Preenchido com a URL do Magic Link em caso de falha final, para ação manual do admin.")
    String fallbackMagicLink
) {}