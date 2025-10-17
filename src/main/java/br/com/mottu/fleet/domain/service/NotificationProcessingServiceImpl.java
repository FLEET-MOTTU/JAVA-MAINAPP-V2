package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;
import br.com.mottu.fleet.infrastructure.router.NotificationServiceRouter;
import br.com.mottu.fleet.infrastructure.websocket.WebSocketNotificationService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementação do serviço de processamento assíncrono para status de notificação
 * Consumer para os eventos de status enviados pelo Twilio
 * em segundo plano para não bloquear a resposta do webhook
 */
@Service
public class NotificationProcessingServiceImpl implements NotificationProcessingService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessingServiceImpl.class);

    private final TokenAcessoRepository tokenAcessoRepository;
    private final String baseUrl;
    private final NotificationServiceRouter notificationRouter;
    private final WebSocketNotificationService webSocketService;

    public NotificationProcessingServiceImpl(TokenAcessoRepository tokenAcessoRepository,
                                           NotificationServiceRouter notificationRouter,
                                           WebSocketNotificationService webSocketService,
                                           @Value("${application.base-url}") String baseUrl) {
        this.tokenAcessoRepository = tokenAcessoRepository;
        this.notificationRouter = notificationRouter;
        this.webSocketService = webSocketService;
        this.baseUrl = baseUrl;
    }

    /**
     * Processa o status de uma mensagem do Twilio
     * Verifica se o status da mensagem é de falha e, caso seja, aciona o
     * fallback para enviar o Magic Link por e-mail.
     *
     * @param messageSid O ID da mensagem do Twilio (Message SID).
     * @param messageStatus O status da mensagem (ex: "failed", "undelivered").
     */    
    @Override
    @Transactional
    public void processarStatusDaMensagem(String messageSid, String messageStatus) {
        log.info("Processando status recebido. SID: {}, Status: {}", messageSid, messageStatus);

        try {
            TokenAcesso token = null;
            // 1. TENTA ENCONTRAR O TOKEN PRIMEIRO (com retentativas)
            for (int i = 0; i < 3; i++) {
                var tokenOpt = tokenAcessoRepository.findByTwilioMessageSid(messageSid);
                if (tokenOpt.isPresent()) {
                    token = tokenOpt.get();
                    log.info("Token encontrado na tentativa {} para o SID: {}", i + 1, messageSid);
                    break; // Sai do loop assim que encontrar
                }
                log.warn("Tentativa {}: Token para SID {} ainda não visível. Aguardando 500ms.", i + 1, messageSid);
                Thread.sleep(500);
            }

            // 2. SE O TOKEN NÃO FOI ENCONTRADO APÓS AS TENTATIVAS, LOGA O ERRO E SAI.
            if (token == null) {
                log.error("FALHA CRÍTICA: Token não encontrado para o SID {} após 3 tentativas. Impossível processar status.", messageSid);
                return; // Não podemos fazer nada sem o token
            }

            // 3. SE O TOKEN FOI ENCONTRADO, AGORA SIM VERIFICAMOS O STATUS E AGIMOS.
            Funcionario funcionario = token.getFuncionario();

            if ("delivered".equalsIgnoreCase(messageStatus)) {
                // CASO 1: WhatsApp entregue com sucesso!
                String msg = String.format("Link de acesso enviado com sucesso para %s via WhatsApp.", funcionario.getNome());
                webSocketService.sendNotificationStatus(funcionario, "WHATSAPP_DELIVERED", msg, null);
                log.info("Notificação WebSocket WHATSAPP_DELIVERED enviada para Pateo {}", funcionario.getPateo().getId());
            
            } else if ("failed".equalsIgnoreCase(messageStatus) || "undelivered".equalsIgnoreCase(messageStatus)) {
                // CASO 2: WhatsApp falhou! Acionando fallback de e-mail.
                log.warn("Mensagem WhatsApp (SID: {}) falhou! Acionando fallback de e-mail.", messageSid);
                String msg = String.format("Falha ao enviar via WhatsApp para %s. Tentando fallback por e-mail...", funcionario.getNome());
                
                webSocketService.sendNotificationStatus(funcionario, "WHATSAPP_FAILED_EMAILING", msg, null);
                log.info("Notificação WebSocket WHATSAPP_FAILED_EMAILING enviada para Pateo {}", funcionario.getPateo().getId());

                enviarEmailDeFallback(token); 
            } else {
                // CASO 3 (Opcional): Outros status como 'sent', 'read', etc.
                // Por enquanto, vamos apenas logar, mas poderíamos enviar um status para o WebSocket se quisesse.
                log.info("Status intermediário [{}] recebido para SID: {}. Nenhuma ação de fallback necessária.", messageStatus, messageSid);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrompida durante a espera pelo token para o SID: {}", messageSid, e);
        } catch (Exception e) {
            log.error("Erro inesperado ao processar status da mensagem para SID: {}", messageSid, e);
            // Considerar mover a mensagem para a Dead Letter Queue aqui também, se possível
            // (exigiria passar o 'context' do listener ou refatorar).
        }
    }

    private void enviarEmailDeFallback(TokenAcesso tokenAcesso) {
        Funcionario funcionario = tokenAcesso.getFuncionario(); 
        String magicLinkUrl = baseUrl + "/auth/validar-token?valor=" + tokenAcesso.getToken();
        
        log.info("Enviando e-mail de fallback para: {}", funcionario.getEmail());
        notificationRouter.getService("sendGridEmailNotificationServiceImpl")
            .enviarMagicLink(funcionario, magicLinkUrl);
    }
}    