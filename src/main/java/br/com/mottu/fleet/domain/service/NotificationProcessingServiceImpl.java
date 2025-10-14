package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Service
public class NotificationProcessingServiceImpl implements NotificationProcessingService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessingServiceImpl.class);

    private final TokenAcessoRepository tokenAcessoRepository;
    private final NotificationService emailNotificationService;
    private final String baseUrl;
    private final ExecutorService emailExecutor;

    public NotificationProcessingServiceImpl(TokenAcessoRepository tokenAcessoRepository,
                                           @Qualifier("sendGridEmailNotificationServiceImpl") NotificationService emailNotificationService,
                                           @Value("${application.base-url}") String baseUrl) {
        this.tokenAcessoRepository = tokenAcessoRepository;
        this.emailNotificationService = emailNotificationService;
        this.baseUrl = baseUrl;

        // Configuração do "vigia" da thread para pegar erros fatais
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("email-fallback-thread");
            thread.setUncaughtExceptionHandler((t, e) -> {
                System.err.println("--- ERRO FATAL NÃO CAPTURADO NA THREAD " + t.getName() + " ---");
                e.printStackTrace(System.err);
            });
            return thread;
        };
        this.emailExecutor = Executors.newSingleThreadExecutor(threadFactory);
    }

    @Override
    public void processarStatusDaMensagem(String messageSid, String messageStatus) {
        if ("failed".equalsIgnoreCase(messageStatus) || "undelivered".equalsIgnoreCase(messageStatus)) {
            log.warn("Mensagem do WhatsApp (SID: {}) falhou! Submetendo tarefa de fallback para depuração.", messageSid);

            emailExecutor.submit(() -> {
                try {
                    log.info("Thread de fallback iniciada. Buscando token com SID: {}", messageSid);
                    
                    // A MUDANÇA ESTÁ AQUI:
                    // 1. Chamamos o método que retorna a entidade diretamente.
                    TokenAcesso tokenAcesso = tokenAcessoRepository.findByTwilioMessageSidWithFuncionario(messageSid);

                    // 2. Fazemos a verificação de nulo manualmente.
                    if (tokenAcesso != null) {
                        System.err.println("DEBUG: PONTO DE CONTROLE 2 - Token encontrado. Acessando dados...");
                        
                        Funcionario funcionario = tokenAcesso.getFuncionario(); // Esta linha agora é segura
                        System.err.println("DEBUG: PONTO DE CONTROLE 3 - Objeto Funcionario acessado. Nome: " + funcionario.getNome());
                        
                        String magicLinkUrl = baseUrl + "/auth/validar-token?valor=" + tokenAcesso.getToken();
                        System.err.println("DEBUG: PONTO DE CONTROLE 4 - URL do Magic Link construída.");
                        
                        emailNotificationService.enviarMagicLink(funcionario, magicLinkUrl);
                        System.err.println("DEBUG: PONTO DE CONTROLE 5 - Chamada ao serviço de e-mail CONCLUÍDA.");
                    } else {
                        log.warn("Nenhum TokenAcesso encontrado para o MessageSID: {}", messageSid);
                    }
                } catch (Throwable t) {
                    System.err.println("--- ERRO FINALMENTE CAPTURADO DENTRO DO RUNNABLE ---");
                    t.printStackTrace(System.err);
                }
                System.err.println("DEBUG: PONTO DE CONTROLE 6 - Fim da execução da thread.");
            });
        }
    }
}