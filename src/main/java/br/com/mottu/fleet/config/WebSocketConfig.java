package br.com.mottu.fleet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import br.com.mottu.fleet.infrastructure.websocket.AdminNotificationSocketHandler;

import org.springframework.lang.NonNull;


/**
 * Habilita o suporte a WebSocket no Spring.
 * Define quais handlers são responsáveis por quais endpoints.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AdminNotificationSocketHandler adminNotificationSocketHandler;

    /**
     * Construtor para injetar o handler principal do WebSocket.
     * @param adminNotificationSocketHandler O bean do handler de notificações.
     */
    public WebSocketConfig(AdminNotificationSocketHandler adminNotificationSocketHandler) {
        this.adminNotificationSocketHandler = adminNotificationSocketHandler;
    }


    /**
     * Registra os WebSocket handlers nos respectivos endpoints.
     * @param registry O registro onde os handlers são configurados.
     */
    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(adminNotificationSocketHandler, "/ws/notifications")
                .setAllowedOrigins("*"); // TODO: trocar para URL front
    }
}