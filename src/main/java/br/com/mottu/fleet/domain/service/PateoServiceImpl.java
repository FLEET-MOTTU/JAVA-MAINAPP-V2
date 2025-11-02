package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.web.FuncionarioViewModel;
import br.com.mottu.fleet.application.dto.web.OnboardingRequest;
import br.com.mottu.fleet.application.dto.web.PateoViewModel;
import br.com.mottu.fleet.application.dto.integration.PateoSyncPayload;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.entity.Zona;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;
import br.com.mottu.fleet.infrastructure.publisher.InterServiceEventPublisher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.IOException;


/**
 * Implementação do serviço de domínio que contém as regras de negócio
 * para o gerenciamento de Pátios.
 */
@Service
public class PateoServiceImpl implements PateoService {

    private final PateoRepository pateoRepository;
    private final TokenAcessoRepository tokenAcessoRepository;
    private final String baseUrl;
    private final StorageService storageService;
    private final InterServiceEventPublisher eventPublisher;
    
    public PateoServiceImpl(PateoRepository pateoRepository,
                            TokenAcessoRepository tokenAcessoRepository,
                            StorageService storageService,
                            @Value("${application.base-url}") String baseUrl,
                            InterServiceEventPublisher eventPublisher) {
        this.pateoRepository = pateoRepository;
        this.tokenAcessoRepository = tokenAcessoRepository;
        this.baseUrl = baseUrl;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
    }


    /**
     * Lista todos os pátios que estão com status ATIVO.
     * @return Uma lista de entidades Pateo.
     */
    @Override
    public List<Pateo> listarTodosAtivos() {
        return pateoRepository.findAllByStatus(Status.ATIVO);
    }


    /**
     * Cria um novo pátio e o associa a um administrador.
     * Chamado pelo fluxo de onboarding.
     * @param request DTO com os dados do novo pátio (nome).
     * @param adminResponsavel A entidade UsuarioAdmin que gerenciará este pátio.
     * @return A entidade Pateo salva.
     */
    @Override
    @Transactional
    public Pateo criarPateo(OnboardingRequest request, UsuarioAdmin adminResponsavel) {
        Pateo novoPateo = new Pateo();
        novoPateo.setNome(request.getNomePateo());
        novoPateo.setGerenciadoPor(adminResponsavel);
        novoPateo.setStatus(Status.ATIVO);
        
        Pateo pateoSalvo = pateoRepository.save(novoPateo);
        
        return pateoSalvo;
    }


    /**
     * Busca os detalhes de um pátio (incluindo zonas) para a API REST.
     * Este método valida se o admin logado tem permissão para acessar o pátio solicitado.
     *
     * @param pateoId O UUID do pátio a ser buscado.
     * @param adminLogado O admin de pátio autenticado.
     * @return A entidade Pateo com a coleção de Zonas carregada.
     * @throws SecurityException Se o pátio não pertencer ao admin.
     * @throws ResourceNotFoundException Se o pátio não for encontrado.
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
     * Prepara o ViewModel para a tela de detalhes do pátio no painel do Super Admin.
     * Este método é otimizado para evitar o problema N+1 ao buscar os Magic Links.
     *
     * @param pateoId O UUID do pátio.
     * @return Um PateoViewModel preenchido.
     * @throws ResourceNotFoundException Se o pátio não for encontrado.
     */
    @Override
    public PateoViewModel prepararViewModelDeDetalhes(UUID pateoId) {
        // 1. Busca o pátio com todas as suas coleções (funcionários e zonas)
        Pateo pateo = pateoRepository.findPateoWithDetailsById(pateoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pátio com ID " + pateoId + " não encontrado."));

        // 2. Busca todos os tokens válidos para todos os funcionários deste pátio
        List<TokenAcesso> tokensValidos = tokenAcessoRepository
            .findAllValidTokensByFuncionarioList(pateo.getFuncionarios(), Instant.now());

        // 3. Mapeia os tokens por Funcionario ID para acesso rápido
        Map<UUID, TokenAcesso> tokenMap = tokensValidos.stream()
            .collect(Collectors.toMap(
                token -> token.getFuncionario().getId(), // Chave: ID do Funcionario
                token -> token,                         // Valor: O objeto TokenAcesso
                (primeiro, segundo) -> primeiro          // Em caso de duplicata, fica com o primeiro
            ));

        // 4. Monta os ViewModels
        List<FuncionarioViewModel> funcionariosComLink = pateo.getFuncionarios().stream()
                .map(funcionario -> {
                    Optional<String> linkUrl = Optional.ofNullable(tokenMap.get(funcionario.getId()))
                            .map(this::buildMagicLinkUrl);
                    return new FuncionarioViewModel(funcionario, linkUrl);
                }).toList();

        List<Zona> zonaList = new ArrayList<>(pateo.getZonas());
        
        return new PateoViewModel(pateo, funcionariosComLink, zonaList);
    }

    
    /**
     * Busca um pátio pelo ID e já carrega suas zonas (visão do Super Admin).
     * @param pateoId O UUID do pátio.
     * @return Um Optional contendo o Pateo com suas Zonas.
     */
    @Override
    public Optional<Pateo> buscarPorIdComZonas(UUID pateoId) {
        return pateoRepository.findPateoWithZonasById(pateoId);
    }


    /**
     * Atualiza a planta baixa de um pátio.
     * Esta operação é chamada pelo Super Admin.
     * @param pateoId O ID do pátio a ser atualizado.
     * @param arquivoPlanta O novo arquivo de imagem.
     * @return O Pátio com a URL da planta atualizada.
     * @throws IOException Se o upload falhar.
     * @throws BusinessException Se o arquivo estiver vazio.
     * @throws ResourceNotFoundException Se o pátio não for encontrado.
     */
    @Override
    @Transactional
    public Pateo atualizarPlantaBaixa(UUID pateoId, MultipartFile arquivoPlanta, Integer largura, Integer altura) throws IOException {

        if (arquivoPlanta == null || arquivoPlanta.isEmpty()) {
            throw new BusinessException("O arquivo da planta não pode ser vazio.");
        }
        if (largura == null || altura == null || largura <= 0 || altura <= 0) {
            throw new BusinessException("As dimensões (largura e altura) são obrigatórias e devem ser maiores que zero.");
        }

        Pateo pateo = pateoRepository.findById(pateoId)
            .orElseThrow(() -> new ResourceNotFoundException("Pátio com ID " + pateoId + " não encontrado."));

        String novaPlantaUrl = storageService.upload("plantas", arquivoPlanta);

        pateo.setPlantaBaixaUrl(novaPlantaUrl);
        pateo.setPlantaLargura(largura);
        pateo.setPlantaAltura(altura);
        
        Pateo pateoAtualizado = pateoRepository.save(pateo);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                PateoSyncPayload payload = new PateoSyncPayload(pateoAtualizado);
                eventPublisher.publishEvent(payload, "PATEO_ATUALIZADO");
            }
        });

        return pateoAtualizado;
    }


    // Métodos Auxiliares

    /**
     * Método auxiliar para buscar o Pátio ATIVO associado a um admin logado.
     * Centraliza a regra de negócio de que um admin deve ter um pátio ativo.
     */
    private Pateo getPateoDoAdmin(UsuarioAdmin adminLogado) {
        return pateoRepository.findAllByGerenciadoPorId(adminLogado.getId())
                .stream()
                .filter(pateo -> pateo.getStatus() == Status.ATIVO) 
                .findFirst()
                .orElseThrow(() -> new BusinessException("Admin não está associado a nenhum pátio ativo."));
    }

    /**
     * Constrói a URL do Magic Link a partir do token de acesso.
     */
    private String buildMagicLinkUrl(TokenAcesso token) {
        return baseUrl + "/auth/validar-token?valor=" + token.getToken();
    }
}