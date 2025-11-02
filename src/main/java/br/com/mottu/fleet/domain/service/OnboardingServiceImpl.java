package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.application.dto.web.OnboardingRequest;
import br.com.mottu.fleet.application.dto.integration.PateoSyncPayload;
import br.com.mottu.fleet.infrastructure.publisher.InterServiceEventPublisher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


/**
 * Serviço de domínio que orquestra o processo de "Onboarding".
 * Implementa o caso de uso de criação de uma nova unidade Mottu completa,
 * que envolve a criação de um Pátio e seu Administrador principal
 * de forma atômica (transacional).
 */
@Service
public class OnboardingServiceImpl implements OnboardingService {

    private final UsuarioAdminService usuarioAdminService;
    private final PateoService pateoService;
    private final InterServiceEventPublisher eventPublisher;

    public OnboardingServiceImpl(UsuarioAdminService usuarioAdminService, 
                                 PateoService pateoService,
                                 InterServiceEventPublisher eventPublisher) {
        this.usuarioAdminService = usuarioAdminService;
        this.pateoService = pateoService;
        this.eventPublisher = eventPublisher;
    }


    /**
     * Orquestra o processo de onboarding de uma nova unidade Mottu.
     * Regra de Negócio: O onboarding consiste em duas etapas atômicas:
     * 1. Criar um novo Administrador de Pátio.
     * 2. Criar um novo Pátio e associá-lo a este administrador recém-criado.
     * 3. Publicar evento de criação de um pátio no publisher para sincronização com API de C#
     * @param request DTO contendo os dados para a criação da unidade e do administrador.
     */
    @Override
    @Transactional
    public void executar(OnboardingRequest request) {
        UsuarioAdmin adminSalvo = usuarioAdminService.criarAdminDePateo(request);
        Pateo pateoSalvo = pateoService.criarPateo(request, adminSalvo);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                PateoSyncPayload payload = new PateoSyncPayload(pateoSalvo);
                eventPublisher.publishEvent(payload, "PATEO_CRIADO");
            }
        });
    }
}
