package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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
    @Transactional
    public void processarStatusDaMensagem(String messageSid, String messageStatus) {
        log.info("Webhook de status recebido. SID: {}, Status: {}", messageSid, messageStatus);

        if ("failed".equalsIgnoreCase(messageStatus) || "undelivered".equalsIgnoreCase(messageStatus)) {
            log.warn("Mensagem (SID: {}) falhou! Iniciando busca pelo token com retentativas.", messageSid);

            try {
                // Tenta encontrar o token por até 3 vezes, com um intervalo de 500ms
                for (int i = 0; i < 3; i++) {
                    var tokenOpt = tokenAcessoRepository.findByTwilioMessageSid(messageSid);
                    if (tokenOpt.isPresent()) {
                        log.info("Token encontrado na tentativa {} para o SID: {}", i + 1, messageSid);
                        enviarEmailDeFallback(tokenOpt.get());
                        return; // Sucesso, sai do método
                    }
                    log.warn("Tentativa {}: Token para SID {} ainda não visível. Aguardando 500ms.", i + 1, messageSid);
                    Thread.sleep(500); // Espera meio segundo
                }
                log.error("FALHA DEFINITIVA: Token não encontrado para o SID {} após 3 tentativas.", messageSid);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrompida durante a espera pelo token para o SID: {}", messageSid, e);
            }
        }
    }

    private void enviarEmailDeFallback(TokenAcesso tokenAcesso) {
        // A lógica de recuperação do funcionário e envio do e-mail
        // LazyInitializationException pode ocorrer aqui se o método não for transacional.
        // Se o erro voltar, adicione @Transactional no método processarStatusDaMensagem.
        Funcionario funcionario = tokenAcesso.getFuncionario(); 
        String magicLinkUrl = baseUrl + "/auth/validar-token?valor=" + tokenAcesso.getToken();
        
        log.info("Enviando e-mail de fallback para: {}", funcionario.getEmail());
        emailNotificationService.enviarMagicLink(funcionario, magicLinkUrl);
    }

}    