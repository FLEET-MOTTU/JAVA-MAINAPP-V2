package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
    private final NotificationService emailNotificationService;
    private final String baseUrl;

    public NotificationProcessingServiceImpl(TokenAcessoRepository tokenAcessoRepository,
                                           @Qualifier("sendGridEmailNotificationServiceImpl") NotificationService emailNotificationService,
                                           @Value("${application.base-url}") String baseUrl) {
        this.tokenAcessoRepository = tokenAcessoRepository;
        this.emailNotificationService = emailNotificationService;
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
    @Async
    public void processarStatusDaMensagem(String messageSid, String messageStatus) {
        log.info("Webhook de status recebido do Twilio. MessageSID: {}, Status: {}", messageSid, messageStatus);

        if ("failed".equalsIgnoreCase(messageStatus) || "undelivered".equalsIgnoreCase(messageStatus)) {
            log.warn("Mensagem do WhatsApp (SID: {}) falhou! Acionando fallback de e-mail.", messageSid);

            tokenAcessoRepository.findByTwilioMessageSid(messageSid).ifPresent(tokenAcesso -> {
                Funcionario funcionario = tokenAcesso.getFuncionario();
                String magicLinkUrl = baseUrl + "/auth/validar-token?valor=" + tokenAcesso.getToken();
                
                log.info("Enviando e-mail de fallback para: {}", funcionario.getEmail());
                emailNotificationService.enviarMagicLink(funcionario, magicLinkUrl);
            });
        }
    }
}