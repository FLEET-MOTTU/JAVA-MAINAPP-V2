package br.com.mottu.fleet.infrastructure.router;

import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.domain.service.NotificationService;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Orquestrador assíncrono para tarefas de notificação.
 * Recebe uma solicitação de notificação da thread principal
 * e executa em uma thread separada (@Async),
 * permitindo que a API retorne o usuário criado imediatamente.
 */
@Service
public class AsyncNotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AsyncNotificationOrchestrator.class);

    private final NotificationService notificationService;
    private final FuncionarioRepository funcionarioRepository;

    /**
     * @param notificationService O serviço de notificação primário (marcado com @Primary, neste caso, o TwilioWhatsApp).
     * @param funcionarioRepository Repositório para buscar os dados do funcionário na nova thread.
     */
    public AsyncNotificationOrchestrator(NotificationService notificationService, FuncionarioRepository funcionarioRepository) {
        this.notificationService = notificationService;
        this.funcionarioRepository = funcionarioRepository;
    }


    /**
     * Dispara o processo de envio de notificação (Magic Link).
     * @param funcionarioId O ID do funcionário.
     * @param magicLink A URL do Magic Link que foi gerada na thread principal.
     */
    @Async
    public void dispararNotificacaoPosCriacao(UUID funcionarioId, String magicLink) {
        log.info("Iniciando envio assíncrono de notificação para o funcionário ID: {}", funcionarioId);

        funcionarioRepository.findById(funcionarioId).ifPresent(funcionario -> {
            try {
                // Chama o serviço de notificação primário (Twilio WhatsApp)
                notificationService.enviarMagicLink(funcionario, magicLink);
                log.info("Chamada de notificação assíncrona para o funcionário ID {} concluída.", funcionarioId);
            } catch (Exception e) {
                log.error("Erro durante o envio assíncrono da notificação para o funcionário ID {}", funcionarioId, e);
            }
        });
        
    }
}