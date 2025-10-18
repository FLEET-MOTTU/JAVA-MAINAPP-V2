package br.com.mottu.fleet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import br.com.mottu.fleet.infrastructure.websocket.AdminNotificationSocketHandler;

import org.springframework.lang.NonNull;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AdminNotificationSocketHandler adminNotificationSocketHandler;

    public WebSocketConfig(AdminNotificationSocketHandler adminNotificationSocketHandler) {
        this.adminNotificationSocketHandler = adminNotificationSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(adminNotificationSocketHandler, "/ws/notifications")
                .setAllowedOrigins("*"); // TO-DO: trocar para URL front
    }
}