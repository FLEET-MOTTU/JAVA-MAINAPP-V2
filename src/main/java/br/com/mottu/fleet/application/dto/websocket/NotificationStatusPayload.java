package br.com.mottu.fleet.application.dto.websocket;

import java.util.UUID;

public record NotificationStatusPayload(
    String type,
    UUID pateoId,
    UUID funcionarioId,
    String status,
    String message,
    String fallbackMagicLink
) {}
