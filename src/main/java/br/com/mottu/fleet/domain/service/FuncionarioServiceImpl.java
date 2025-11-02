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
import br.com.mottu.fleet.infrastructure.publisher.InterServiceEventPublisher;

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
 * Implementação do serviço de domínio que contém as regras de negócio
 * para o gerenciamento de Funcionários.
 * Esta classe é usada pela API REST do Admin de Pátio.
 */
@Service
public class FuncionarioServiceImpl implements FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final PateoRepository pateoRepository;
    private final MagicLinkService magicLinkService;
    private final StorageService storageService;
    private final AsyncNotificationOrchestrator asyncOrchestrator;
    private final InterServiceEventPublisher eventPublisher;

    public FuncionarioServiceImpl(FuncionarioRepository funcionarioRepository,
                                  PateoRepository pateoRepository,
                                  MagicLinkService magicLinkService,
                                  StorageService storageService,
                                  AsyncNotificationOrchestrator asyncOrchestrator,
                                  InterServiceEventPublisher eventPublisher) {
        this.funcionarioRepository = funcionarioRepository;
        this.pateoRepository = pateoRepository;
        this.magicLinkService = magicLinkService;
        this.storageService = storageService;
        this.asyncOrchestrator = asyncOrchestrator;
        this.eventPublisher = eventPublisher;
    }


    /**
     * Cria um novo funcionário, faz upload de foto (se fornecida), e agenda as
     * notificações assíncronas (Magic Link e Sincronização de C#) para
     * dispararem somente após o commit da transação.
     *
     * @param request DTO com os dados do novo funcionário.
     * @param foto Arquivo de foto opcional.
     * @param adminLogado O admin de pátio autenticado.
     * @return A entidade Funcionario recém-criada e salva.
     * @throws IOException Se houver um erro no upload do arquivo.
     * @throws BusinessException Se o e-mail ou telefone já estiverem em uso.
     */
    @Override
    @Transactional
    public Funcionario criar(FuncionarioCreateRequest request, MultipartFile foto, UsuarioAdmin adminLogado) throws IOException {
        Pateo pateo = getPateoDoAdmin(adminLogado);

        // Apesar da validação pela DTO adicionar um fail-fast pra não poluir a fila do C#
        if (funcionarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("O e-mail fornecido já está em uso.");
        }
        if (funcionarioRepository.existsByTelefone(request.getTelefone())) {
            throw new BusinessException("O telefone fornecido já está em uso.");
        }

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
                // 1: Dispara a notificação do Magic Link (WhatsApp + Fallback)
                asyncOrchestrator.dispararNotificacaoPosCriacao(funcionarioSalvo.getId(), link);
                
                // 2: Dispara o evento de sincronização para a API de C#
                eventPublisher.publishEvent(funcionarioSalvo, "FUNCIONARIO_CRIADO");
            }
        });
        
        return funcionarioSalvo;
    }


    /**
     * Lista os funcionários de um pátio com base em filtros.
     * Restrito ao pátio do administrador logado.
     * @param adminLogado O admin de pátio autenticado.
     * @param status Filtro opcional por status. Se nulo, busca apenas ATIVOS.
     * @param cargo Filtro opcional por cargo.
     * @return Uma lista de Funcionarios.
     */
    @Override
    public List<Funcionario> listarPorAdminEfiltros(UsuarioAdmin adminLogado, Status status, Cargo cargo) {
        Pateo pateo = getPateoDoAdmin(adminLogado);
        Status statusFiltrar = (status == null) ? Status.ATIVO : status;
        
        // Usa a Specification para montar a query de filtro
        Specification<Funcionario> spec = FuncionarioSpecification.comFiltros(pateo.getId(), statusFiltrar, cargo);
        return funcionarioRepository.findAll(spec);
    }


    /**
     * Atualiza os dados de um funcionário e dispara um evento de sincronização.
     * @param id O UUID do funcionário a ser atualizado.
     * @param request DTO com os novos dados.
     * @param adminLogado O admin de pátio autenticado.
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
        funcionario.setCargo(Cargo.valueOf(request.getCargo()));
        funcionario.setStatus(Status.valueOf(request.getStatus()));

        Funcionario funcionarioAtualizado = funcionarioRepository.save(funcionario);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(funcionarioAtualizado, "FUNCIONARIO_ATUALIZADO");
            }
        });

        return funcionarioAtualizado;
    }


    /**
     * Atualiza a foto de um funcionário, faz o upload, e dispara um evento de sincronização.
     * @param id O UUID do funcionário.
     * @param foto O novo arquivo de imagem.
     * @param adminLogado O admin de pátio autenticado.
     * @return A entidade Funcionario atualizada com a nova URL da foto.
     * @throws IOException Se houver um erro no upload do arquivo.
     */
    @Override
    @Transactional
    public Funcionario atualizarFoto(UUID id, MultipartFile foto, UsuarioAdmin adminLogado) throws IOException {
        Pateo pateoDoAdmin = getPateoDoAdmin(adminLogado);
        Funcionario funcionario = findFuncionarioByIdAndCheckPateo(id, pateoDoAdmin.getId());

        if (foto == null || foto.isEmpty()) {
            throw new BusinessException("O arquivo da foto não pode ser vazio.");
        }

        // Chama o serviço de infraestrutura para fazer o upload
        String fotoUrl = storageService.upload("fotos", foto);

        funcionario.setFotoUrl(fotoUrl);
        Funcionario funcionarioAtualizadoFoto = funcionarioRepository.save(funcionario);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(funcionarioAtualizadoFoto, "FUNCIONARIO_ATUALIZADO_FOTO");
            }
        });

        return funcionarioAtualizadoFoto;
    }


    /**
     * Realiza o "soft delete" de um funcionário e dispara um evento de sincronização.
     * @param id O UUID do funcionário a ser desativado.
     * @param adminLogado O admin de pátio autenticado.
     */
    @Override
    @Transactional
    public void desativar(UUID id, UsuarioAdmin adminLogado) {
        Pateo pateoDoAdmin = getPateoDoAdmin(adminLogado);
        Funcionario funcionario = findFuncionarioByIdAndCheckPateo(id, pateoDoAdmin.getId());

        funcionario.setStatus(Status.REMOVIDO);
        Funcionario funcionarioDesativado = funcionarioRepository.save(funcionario);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(funcionarioDesativado, "FUNCIONARIO_DESATIVADO");
            }
        });
    }


    /**
     * Reativa um funcionário (status REMOVIDO para ATIVO) e dispara um evento de sincronização.
     * @param id O UUID do funcionário a ser reativado.
     * @param adminLogado O admin de pátio autenticado.
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

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(funcionarioReativado, "FUNCIONARIO_REATIVADO");
            }
        });
    }    

    
    // --- Métodos Auxiliares ---

    /**
     * Método auxiliar privado para buscar o Pátio associado ao admin logado.
     * Garante que o admin de pátio só possa atuar dentro do seu próprio pátio.
     */
    private Pateo getPateoDoAdmin(UsuarioAdmin adminLogado) {
        return pateoRepository.findFirstByGerenciadoPorId(adminLogado.getId())
                .orElseThrow(() -> new BusinessException("Admin não está associado a nenhum pátio."));
    }


    /**
     * Método auxiliar privado para buscar um funcionário e, ao mesmo tempo,
     * validar se ele pertence ao pátio do admin logado.
     */
    private Funcionario findFuncionarioByIdAndCheckPateo(UUID funcionarioId, UUID pateoId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário com ID " + funcionarioId + " não encontrado."));

        if (!funcionario.getPateo().getId().equals(pateoId)) {
            throw new SecurityException("Acesso negado: este funcionário não pertence ao seu pátio.");
        }
        return funcionario;
    }

}