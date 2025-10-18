package br.com.mottu.fleet.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URI;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handler principal para as conexões WebSocket dos Admins de Pátio.
 * Gerencia o ciclo de vida das sessões e envia notificações direcionadas 
 * para consumo do frontend sobre o status do envio do Magic Link.
 */
@Component
public class AdminNotificationSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationSocketHandler.class);

    private final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AdminNotificationSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    /**
     * Chamado após uma nova conexão WebSocket ser estabelecida.
     * PateoId é extraído das claims para associar a sessão ao pátio correto.
     * @param session A nova sessão WebSocket.
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        log.info("Nova conexão WebSocket estabelecida: {}", session.getId());

        UUID pateoId = getPateoIdFromSession(session);
        if (pateoId != null) {
            sessions.put(pateoId, session);
            log.info("WebSocket: Conexão estabelecida para o Pátio ID: {}", pateoId);
        } else {
            log.warn("WebSocket: Conexão recebida sem um pateoId válido. Fechando sessão.");
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Pateo ID é obrigatório"));
        }
    }


    /**
     * Chamado após uma conexão WebSocket ser encerrada.
     * Remove a sessão da lista de conexões ativas para evitar memory leak.
     * @param session A sessão que foi fechada.
     * @param status O motivo do fechamento.
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        log.info("Conexão WebSocket encerrada: {}. Status: {}", session.getId(), status.getCode());

        UUID pateoId = getPateoIdFromSession(session);
        if (pateoId != null) {
            sessions.remove(pateoId);
            log.info("WebSocket: Conexão fechada para o Pátio ID: {}. Motivo: {}", pateoId, status);
        }
    }


    /**
     * Envia uma notificação em tempo real para o admin de um pátio específico.
     * @param pateoId O ID do pátio para o qual a notificação se destina.
     * @param payload O objeto de dados a ser enviado como JSON.
     */
    public void sendMessageToPateo(UUID pateoId, Object payload) {
        WebSocketSession session = sessions.get(pateoId);
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(payload);
                session.sendMessage(new TextMessage(message));
                log.info("WebSocket: Mensagem enviada para o Pátio ID: {}", pateoId);
            } catch (IOException e) {
                log.error("WebSocket: Falha ao enviar mensagem para o Pátio ID: {}", pateoId, e);
            }
        } else {
            log.warn("WebSocket: Nenhuma sessão ativa encontrada para o Pátio ID: {}. Mensagem não enviada.", pateoId);
        }
    }


    /**
     * Método auxiliar para extrair o PateoId dos parâmetros da URI da sessão.
     */
    private @Nullable UUID getPateoIdFromSession(WebSocketSession session) {

        if (session.getUri() == null) {
            return null;
        }

        URI uri = Objects.requireNonNull(session.getUri());
        String pateoIdStr = UriComponentsBuilder.fromUri(uri).build()
                .getQueryParams().getFirst("pateoId");
                
        try {
            return UUID.fromString(pateoIdStr);
        } catch (Exception e) {
            return null;
        }
    }
    
}