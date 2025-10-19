package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.web.OnboardingRequest;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

    @Autowired
    public OnboardingServiceImpl(UsuarioAdminService usuarioAdminService, PateoService pateoService) {
        this.usuarioAdminService = usuarioAdminService;
        this.pateoService = pateoService;
    }


    /**
     * Orquestra o processo de onboarding de uma nova unidade Mottu.
     * Regra de Negócio: O onboarding consiste em duas etapas atômicas:
     * 1. Criar um novo Administrador de Pátio.
     * 2. Criar um novo Pátio e associá-lo a este administrador recém-criado.
     * A anotação @Transactional garante que ambas as operações sejam concluídas com sucesso,
     * ou nenhuma delas é efetivada, mantendo a consistência dos dados.
     *
     * @param request DTO contendo os dados para a criação da unidade e do administrador.
     */
    @Override
    @Transactional
    public void executar(OnboardingRequest request) {
        UsuarioAdmin adminSalvo = usuarioAdminService.criarAdminDePateo(request);
        pateoService.criarPateo(request, adminSalvo);
    }
    
}