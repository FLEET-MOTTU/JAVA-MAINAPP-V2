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

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

/**
 * Implementação do serviço que contém as regras de negócio para
 * o gerenciamento de Funcionários.
 */
@Service
public class FuncionarioServiceImpl implements FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final PateoRepository pateoRepository;
    private final MagicLinkService magicLinkService;
    private final NotificationService notificationService;

    public FuncionarioServiceImpl(FuncionarioRepository funcionarioRepository,
                                  PateoRepository pateoRepository,
                                  MagicLinkService magicLinkService,
                                  NotificationService notificationService) {
        this.funcionarioRepository = funcionarioRepository;
        this.pateoRepository = pateoRepository;
        this.magicLinkService = magicLinkService;
        this.notificationService = notificationService;
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
    public Funcionario criar(FuncionarioCreateRequest request, UsuarioAdmin adminLogado) {
        Pateo pateo = getPateoDoAdmin(adminLogado);

        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(request.getNome());
        novoFuncionario.setTelefone(request.getTelefone());
        novoFuncionario.setEmail(request.getEmail());
        novoFuncionario.setCodigo("FUNC-" + request.getTelefone());        
        novoFuncionario.setPateo(pateo);
        novoFuncionario.setStatus(Status.ATIVO);        
        novoFuncionario.setCargo(Cargo.valueOf(request.getCargo()));

        Funcionario funcionarioSalvo = funcionarioRepository.save(novoFuncionario);
        String link = magicLinkService.gerarLink(funcionarioSalvo);

          TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.enviarMagicLinkPorWhatsapp(funcionarioSalvo, link);
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
     * Atualiza os dados de um funcionário existente.
     * @param id O UUID do funcionário a ser atualizado.
     * @param request DTO com os novos dados.
     * @param adminLogado O admin de pátio autenticado, para validação de segurança.
     * @return A entidade Funcionario atualizada.
     * @throws SecurityException se o funcionário não pertencer ao pátio do admin.
     * @throws BusinessException se o funcionário já estiver removido.
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

        return funcionarioRepository.save(funcionario);
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
        funcionarioRepository.save(funcionario);
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
        funcionarioRepository.save(funcionario);
    }
}