package br.com.mottu.fleet.infrastructure.router;

import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.domain.service.NotificationService;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AsyncNotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AsyncNotificationOrchestrator.class);

    private final NotificationService notificationService;
    private final FuncionarioRepository funcionarioRepository;

    public AsyncNotificationOrchestrator(NotificationService notificationService, FuncionarioRepository funcionarioRepository) {
        this.notificationService = notificationService;
        this.funcionarioRepository = funcionarioRepository;
    }

    /**
     * Método executado em uma thread separada do pool de tarefas do Spring.
     * Chama o serviço do Twilio pra desafogar a thread principal.
     */
    @Async
    public void dispararNotificacaoPosCriacao(UUID funcionarioId, String magicLink) {
        log.info("Iniciando envio assíncrono de notificação para o funcionário ID: {}", funcionarioId);

        funcionarioRepository.findById(funcionarioId).ifPresent(funcionario -> {
            try {
                notificationService.enviarMagicLink(funcionario, magicLink);
                log.info("Chamada de notificação assíncrona para o funcionário ID {} concluída.", funcionarioId);
            } catch (Exception e) {
                log.error("Erro durante o envio assíncrono da notificação para o funcionário ID {}", funcionarioId, e);
            }
        });
        
    }
}