package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.FuncionarioViewModel;
import br.com.mottu.fleet.application.dto.OnboardingRequest;
import br.com.mottu.fleet.application.dto.PateoViewModel;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.entity.Zona;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PateoServiceImpl implements PateoService {

    private final PateoRepository pateoRepository;
    private final TokenAcessoRepository tokenAcessoRepository;
    private final String baseUrl;
    
    public PateoServiceImpl(PateoRepository pateoRepository, 
                              TokenAcessoRepository tokenAcessoRepository, 
                              @Value("${application.base-url}") String baseUrl) {
        this.pateoRepository = pateoRepository;
        this.tokenAcessoRepository = tokenAcessoRepository;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<Pateo> listarTodosAtivos() {
        return pateoRepository.findAllByStatus(Status.ATIVO);
    }

    @Override
    @Transactional
    public Pateo criarPateo(OnboardingRequest request, UsuarioAdmin adminResponsavel) {
        Pateo novoPateo = new Pateo();
        novoPateo.setNome(request.getNomePateo());
        novoPateo.setGerenciadoPor(adminResponsavel);
        novoPateo.setStatus(Status.ATIVO);
        return pateoRepository.save(novoPateo);
    }

    /**
     * Busca os detalhes de um pátio, incluindo suas zonas, e valida se o admin logado
     * tem permissão de acesso.
     * REGRA DE NEGÓCIO: Um admin só pode acessar detalhes do pátio que ele gerencia.
     */
    @Override
    public Pateo buscarDetalhesDoPateo(UUID pateoId, UsuarioAdmin adminLogado) {
        // Valida se o pátio pertence ao admin que está fazendo a requisição
        Pateo pateoDoAdmin = getPateoDoAdmin(adminLogado);
        if (!pateoDoAdmin.getId().equals(pateoId)) {
            throw new SecurityException("Acesso negado: este pátio não pertence a você.");
        }

        // Usa a query otimizada para buscar o pátio e suas zonas em uma única consulta
        return pateoRepository.findPateoWithZonasById(pateoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pátio com ID " + pateoId + " não encontrado."));
    }

    /**
     * Método para buscar o pátio associado a um admin.
     * Centraliza a regra de negócio de que um admin deve ter um pátio.
     */
    private Pateo getPateoDoAdmin(UsuarioAdmin adminLogado) {
        return pateoRepository.findAllByGerenciadoPorId(adminLogado.getId())
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException("Admin não está associado a nenhum pátio."));
    }
    
    /**
     * Método personalizado para buscar o pátio com suas zonas.
     * Da Bypass nas regras de segurança normais, usado apenas para o super admin.
     */
    @Override
    public Optional<Pateo> buscarPorIdComZonas(UUID pateoId) {
        return pateoRepository.findPateoWithZonasById(pateoId);
    }

    /**
     * Prepara o ViewModel com os detalhes do pátio, incluindo zonas e funcionários.
     * Gera links mágicos para cada funcionário, se houver tokens válidos.
     */
    @Override
    public PateoViewModel prepararViewModelDeDetalhes(UUID pateoId) {
        Pateo pateo = pateoRepository.findPateoWithDetailsById(pateoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pátio com ID " + pateoId + " não encontrado."));

        List<FuncionarioViewModel> funcionariosComLink = pateo.getFuncionarios().stream()
                .map(funcionario -> {
                    Optional<String> linkUrl = tokenAcessoRepository
                            .findFirstByFuncionarioAndUsadoIsFalseAndExpiraEmAfterOrderByCriadoEmDesc(funcionario, Instant.now())
                            .map(this::buildMagicLinkUrl);
                    return new FuncionarioViewModel(funcionario, linkUrl);
                }).toList();

        List<Zona> zonaList = new ArrayList<>(pateo.getZonas());
        
        return new PateoViewModel(pateo, funcionariosComLink, zonaList);
    }

    /** Constrói a URL do link mágico a partir do token de acesso. */
    private String buildMagicLinkUrl(TokenAcesso token) {
        return baseUrl + "/auth/validar-token?valor=" + token.getToken();
    }

}