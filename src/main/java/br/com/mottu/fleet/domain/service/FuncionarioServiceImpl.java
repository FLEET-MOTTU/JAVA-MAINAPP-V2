package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.api.FuncionarioCreateRequest;
import br.com.mottu.fleet.application.dto.api.FuncionarioUpdateRequest;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Cargo;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.specification.FuncionarioSpecification;
import br.com.mottu.fleet.infrastructure.router.AsyncNotificationOrchestrator;
import br.com.mottu.fleet.infrastructure.publisher.FuncionarioEventPublisher;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.io.IOException;

/**
 * Implementação do serviço que contém as regras de negócio para
 * o gerenciamento de Funcionários.
 */
@Service
public class FuncionarioServiceImpl implements FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final PateoRepository pateoRepository;
    private final MagicLinkService magicLinkService;
    private final StorageService storageService;
    private final AsyncNotificationOrchestrator asyncOrchestrator;
    private final FuncionarioEventPublisher eventPublisher;

    public FuncionarioServiceImpl(FuncionarioRepository funcionarioRepository,
                                  PateoRepository pateoRepository,
                                  MagicLinkService magicLinkService,
                                  StorageService storageService,
                                  AsyncNotificationOrchestrator asyncOrchestrator,
                                  FuncionarioEventPublisher eventPublisher) {
        this.funcionarioRepository = funcionarioRepository;
        this.pateoRepository = pateoRepository;
        this.magicLinkService = magicLinkService;
        this.storageService = storageService;
        this.asyncOrchestrator = asyncOrchestrator;
        this.eventPublisher = eventPublisher;
    }


    /**
     * Cria um novo funcionário, gera um Magic Link para seu primeiro acesso e dispara
     * a notificação (via WhatsApp) com o link.
     * @param request DTO com os dados do novo funcionário.
     * @param adminLogado O admin de pátio autenticado que está realizando a operação.
     * @return A entidade Funcionario recém-criada e salva.
     */
    @Override
    @Transactional
    public Funcionario criar(FuncionarioCreateRequest request, MultipartFile foto, UsuarioAdmin adminLogado) throws IOException {
        Pateo pateo = getPateoDoAdmin(adminLogado);

        String fotoUrl = null;
        if (foto != null && !foto.isEmpty()) {
            fotoUrl = storageService.upload("fotos", foto);
        }

        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(request.getNome());
        novoFuncionario.setTelefone(request.getTelefone());
        novoFuncionario.setEmail(request.getEmail());
        novoFuncionario.setFotoUrl(fotoUrl);
        novoFuncionario.setPateo(pateo);
        novoFuncionario.setCargo(Cargo.valueOf(request.getCargo()));
        novoFuncionario.setStatus(Status.ATIVO);
        novoFuncionario.setCodigo("FUNC-" + request.getTelefone());

        Funcionario funcionarioSalvo = funcionarioRepository.save(novoFuncionario);
        String link = magicLinkService.gerarLink(funcionarioSalvo);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncOrchestrator.dispararNotificacaoPosCriacao(funcionarioSalvo.getId(), link);
                eventPublisher.publishFuncionarioEvent(funcionarioSalvo, "FUNCIONARIO_CRIADO");
            }
        });
        
        return funcionarioSalvo;
    }


    /**
     * Lista os funcionários de um pátio com base em filtros opcionais.
     * A busca é restrita ao pátio do administrador que está fazendo a requisição.
     * @param adminLogado O admin de pátio autenticado.
     * @param status Filtro opcional por status do funcionário. Se nulo, busca apenas ATIVOS.
     * @param cargo Filtro opcional por cargo do funcionário.
     * @return Uma lista de entidades Funcionario que correspondem aos filtros.
     */
    @Override
    public List<Funcionario> listarPorAdminEfiltros(UsuarioAdmin adminLogado, Status status, Cargo cargo) {
        Pateo pateo = getPateoDoAdmin(adminLogado);
        Status statusFiltrar = (status == null) ? Status.ATIVO : status;
        Specification<Funcionario> spec = FuncionarioSpecification.comFiltros(pateo.getId(), statusFiltrar, cargo);
        return funcionarioRepository.findAll(spec);
    }


    /**
     * Atualiza os dados textuais de um funcionário existente (nome, email, cargo, etc.).
     * Este método NÃO lida com o upload de arquivos de foto.
     * @param id O UUID do funcionário a ser atualizado.
     * @param request DTO com os novos dados.
     * @param adminLogado O admin de pátio autenticado, para validação de segurança.
     * @return A entidade Funcionario atualizada.
     */
    @Override
    @Transactional
    public Funcionario atualizar(UUID id, FuncionarioUpdateRequest request, UsuarioAdmin adminLogado) {
        Pateo pateoDoAdmin = getPateoDoAdmin(adminLogado);
        Funcionario funcionario = findFuncionarioByIdAndCheckPateo(id, pateoDoAdmin.getId());

        if (funcionario.getStatus() == Status.REMOVIDO) {
            throw new BusinessException("Não é possível alterar um funcionário que já foi removido.");
        }

        funcionario.setNome(request.getNome());
        funcionario.setTelefone(request.getTelefone());
        funcionario.setEmail(request.getEmail());
        funcionario.setFotoUrl(request.getFotoUrl());
        funcionario.setCargo(Cargo.valueOf(request.getCargo()));
        funcionario.setStatus(Status.valueOf(request.getStatus()));

        Funcionario funcionarioAtualizado = funcionarioRepository.save(funcionario);
        eventPublisher.publishFuncionarioEvent(funcionarioAtualizado, "FUNCIONARIO_ATUALIZADO");

        return funcionarioAtualizado;
    }    


    /**
     * Atualiza a foto de um funcionário. Faz o upload do novo arquivo para o
     * storage e salva a URL resultante na entidade do funcionário.
     *
     * @param id O UUID do funcionário a ser atualizado.
     * @param foto O arquivo de imagem (MultipartFile) enviado.
     * @param adminLogado O admin de pátio autenticado, para validação de segurança.
     * @return A entidade Funcionario atualizada com a nova fotoUrl.
     * @throws IOException Se houver um erro no processamento do arquivo.
     */
    @Override
    @Transactional
    public Funcionario atualizarFoto(UUID id, MultipartFile foto, UsuarioAdmin adminLogado) throws IOException {
        Pateo pateoDoAdmin = getPateoDoAdmin(adminLogado);
        Funcionario funcionario = findFuncionarioByIdAndCheckPateo(id, pateoDoAdmin.getId());

        if (foto == null || foto.isEmpty()) {
            throw new BusinessException("O arquivo da foto não pode ser vazio.");
        }

        String fotoUrl = storageService.upload("fotos", foto);

        funcionario.setFotoUrl(fotoUrl);
        Funcionario funcionarioAtualizadoFoto = funcionarioRepository.save(funcionario);
        eventPublisher.publishFuncionarioEvent(funcionarioAtualizadoFoto, "FUNCIONARIO_ATUALIZADO_FOTO");

        return funcionarioAtualizadoFoto;
    }


    /**
     * Realiza o "soft delete" de um funcionário, alterando seu status para REMOVIDO.
     * @param id O UUID do funcionário a ser desativado.
     * @param adminLogado O admin de pátio autenticado, para validação de segurança.
     */
    @Override
    @Transactional
    public void desativar(UUID id, UsuarioAdmin adminLogado) {
        Pateo pateoDoAdmin = getPateoDoAdmin(adminLogado);
        Funcionario funcionario = findFuncionarioByIdAndCheckPateo(id, pateoDoAdmin.getId());

        funcionario.setStatus(Status.REMOVIDO);
        Funcionario funcionarioDesativado = funcionarioRepository.save(funcionario);
        eventPublisher.publishFuncionarioEvent(funcionarioDesativado, "FUNCIONARIO_DESATIVADO");
    }

    /**
     * Reativa um funcionário que foi previamente desativado (soft-deleted).
     * Altera o status do funcionário de REMOVIDO para ATIVO.
     *
     * @param id O UUID do funcionário a ser reativado.
     * @param adminLogado O admin de pátio autenticado, para validação de segurança.
     */
    @Override
    @Transactional
    public void reativar(UUID id, UsuarioAdmin adminLogado) {
        Pateo pateoDoAdmin = getPateoDoAdmin(adminLogado);
        Funcionario funcionario = findFuncionarioByIdAndCheckPateo(id, pateoDoAdmin.getId());

        if (funcionario.getStatus() != Status.REMOVIDO) {
            throw new BusinessException("Este funcionário não está desativado.");
        }

        funcionario.setStatus(Status.ATIVO);
        Funcionario funcionarioReativado = funcionarioRepository.save(funcionario);
        eventPublisher.publishFuncionarioEvent(funcionarioReativado, "FUNCIONARIO_REATIVADO");
    }

    
    // Métodos Auxiliares

    private Pateo getPateoDoAdmin(UsuarioAdmin adminLogado) {
        return pateoRepository.findFirstByGerenciadoPorId(adminLogado.getId())
                .orElseThrow(() -> new BusinessException("Admin não está associado a nenhum pátio."));
    }

    private Funcionario findFuncionarioByIdAndCheckPateo(UUID funcionarioId, UUID pateoId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário com ID " + funcionarioId + " não encontrado."));

        if (!funcionario.getPateo().getId().equals(pateoId)) {
            throw new SecurityException("Acesso negado: este funcionário não pertence ao seu pátio.");
        }
        return funcionario;
    }

}