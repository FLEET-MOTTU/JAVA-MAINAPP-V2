package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.repository.UsuarioAdminRepository;
import br.com.mottu.fleet.domain.repository.AuthCodeRepository;
import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.RefreshTokenRepository;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;
import br.com.mottu.fleet.domain.enums.Role;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.EmailAlreadyExistsException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.application.dto.api.PasswordChangeRequest;
import br.com.mottu.fleet.application.dto.web.OnboardingRequest;
import br.com.mottu.fleet.application.dto.web.UsuarioAdminUpdateRequest;
import br.com.mottu.fleet.application.dto.web.AdminComPateoViewModel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;
import java.util.List;


/**
 * Implementação do serviço que contém as regras de negócio para
 * o gerenciamento de usuários administradores (Super Admins e Admins de Pátio).
 */
@Service
public class UsuarioAdminServiceImpl implements UsuarioAdminService {

    private final UsuarioAdminRepository usuarioAdminRepository;
    private final PateoRepository pateoRepository;
    private final PasswordEncoder passwordEncoder;
    private final FuncionarioRepository funcionarioRepository;
    private final TokenAcessoRepository tokenAcessoRepository;
    private final AuthCodeRepository authCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public UsuarioAdminServiceImpl(UsuarioAdminRepository usuarioAdminRepository,
                                    PateoRepository pateoRepository,
                                    PasswordEncoder passwordEncoder,
                                    FuncionarioRepository funcionarioRepository,
                                    TokenAcessoRepository tokenAcessoRepository,
                                    AuthCodeRepository authCodeRepository,
                                    RefreshTokenRepository refreshTokenRepository) {
        this.usuarioAdminRepository = usuarioAdminRepository;
        this.pateoRepository = pateoRepository;
        this.passwordEncoder = passwordEncoder;
        this.funcionarioRepository = funcionarioRepository;
        this.tokenAcessoRepository = tokenAcessoRepository;
        this.authCodeRepository = authCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }
    

    /**
     * Lista administradores de pátio de forma paginada e com filtro por status.
     * Para cada administrador, busca o nome do pátio que ele gerencia.
     * @param status O status para filtrar a busca (ATIVO, REMOVIDO, SUSPENSO). null? = ATIVO.
     * @param pageable Objeto contendo as informações de paginação.
     * @return Page de ViewModels, contendo os dados do admin e do seu pátio.
     */
    @Override
    public Page<AdminComPateoViewModel> listarAdminsDePateoPaginado(Status status, Pageable pageable) {
        Status statusParaBuscar = (status == null) ? Status.ATIVO : status;
        Page<UsuarioAdmin> adminsPage = usuarioAdminRepository.findAllByRoleAndStatus(Role.PATEO_ADMIN, statusParaBuscar, pageable);

        return adminsPage.map(admin -> {
            String nomePateo = pateoRepository.findFirstByGerenciadoPorId(admin.getId())
                    .map(Pateo::getNome)
                    .orElse("Nenhum pátio associado");
            return new AdminComPateoViewModel(admin, nomePateo);
        });
    }


    /**
     * Realiza o "soft delete" de um administrador e de todos os pátios associados a ele.
     * Regra de Negócio: A desativação de um admin implica na desativação em cascata de suas unidades.
     * O status de ambos é alterado para REMOVIDO.
     * @param id O UUID do administrador a ser desativado.
     * @throws ResourceNotFoundException se o usuário não for encontrado.
     */
    @Override
    @Transactional
    public void desativarPorId(UUID id) {
        UsuarioAdmin admin = findAdminById(id);
        admin.setStatus(Status.REMOVIDO);
        usuarioAdminRepository.save(admin);

        pateoRepository.findAllByGerenciadoPorId(id).forEach(pateo -> {
            pateo.setStatus(Status.REMOVIDO);
            pateoRepository.save(pateo);
        });
    }


    /**
     * Busca uma entidade UsuarioAdmin pelo seu ID.
     * @param id O UUID do administrador.
     * @return Optional contendo o UsuarioAdmin, se encontrado.
     */
    @Override
    public Optional<UsuarioAdmin> buscarPorId(UUID id) {
        return usuarioAdminRepository.findById(id);
    }


    /**
     * Atualiza os dados de um administrador de pátio. Nome e senha podem ser alterados.
     * @param request DTO com os dados da atualização.
     * @throws ResourceNotFoundException se o usuário não for encontrado.
     * @throws BusinessException se a senha nova for igual a atual
     */
    @Override
    @Transactional
    public void atualizar(UsuarioAdminUpdateRequest request) {
        UsuarioAdmin adminExistente = findAdminById(request.getId());
        adminExistente.setNome(request.getNome());

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {            
            if (passwordEncoder.matches(request.getNewPassword(), adminExistente.getPassword())) {
                throw new BusinessException("A nova senha não pode ser igual à senha atual.");
            }            
            adminExistente.setSenha(passwordEncoder.encode(request.getNewPassword()));
        }
        usuarioAdminRepository.save(adminExistente);
    }


    /**
     * Cria uma nova entidade UsuarioAdmin com a role PATEO_ADMIN.
     * Usado pelo fluxo de onboarding.
     * @param request DTO com os dados do novo administrador e pátio.
     * @return A entidade UsuarioAdmin salva.
     * @throws EmailAlreadyExistsException se o email fornecido já estiver em uso.
     */
    @Override
    @Transactional
    public UsuarioAdmin criarAdminDePateo(OnboardingRequest request) {
        usuarioAdminRepository.findByEmail(request.getEmailAdminPateo())
                .ifPresent(usuario -> {
                    throw new EmailAlreadyExistsException("O email informado já está em uso.");
                });

        UsuarioAdmin novoAdminPateo = new UsuarioAdmin();
        novoAdminPateo.setNome(request.getNomeAdminPateo());
        novoAdminPateo.setEmail(request.getEmailAdminPateo());
        novoAdminPateo.setSenha(passwordEncoder.encode(request.getSenhaAdminPateo()));
        novoAdminPateo.setRole(Role.PATEO_ADMIN);
        novoAdminPateo.setStatus(Status.ATIVO);

        return usuarioAdminRepository.save(novoAdminPateo);
    }


    /**
     * Altera a senha de um administrador de pátio já autenticado.
     * @param adminLogado O objeto UserDetails do administrador logado.
     * @param request DTO contendo a senha atual e a nova senha.
     * @throws BusinessException se a senha atual estiver incorreta.
     */
    @Override
    @Transactional
    public void alterarSenha(UsuarioAdmin adminLogado, PasswordChangeRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), adminLogado.getPassword())) {
            throw new BusinessException("A senha atual está incorreta.");
        }
        adminLogado.setSenha(passwordEncoder.encode(request.newPassword()));
        usuarioAdminRepository.save(adminLogado);
    }

    /**
     * Reativa um administrador e seus pátios associados.
     * Regra de Negócio: A reativação do admin cascateia para suas unidades.
     * O status de ambos é alterado para ATIVO.
     * @param id O UUID do administrador a ser reativado.
     * @throws ResourceNotFoundException se o usuário não for encontrado.
     */
    @Override
    @Transactional
    public void reativarPorId(UUID id) {
        UsuarioAdmin admin = findAdminById(id);
        admin.setStatus(Status.ATIVO);
        usuarioAdminRepository.save(admin);

        pateoRepository.findAllByGerenciadoPorId(id).forEach(pateo -> {
            pateo.setStatus(Status.ATIVO);
            pateoRepository.save(pateo);
        });
    }


    /**
     * Realiza o HARD DELETE de um funcionário e todos os seus tokens associados.
     * Esta é uma operação de Super Admin e não deve ser exposta na API pública.
     * @param id O UUID do funcionário a ser deletado.
     */
    @Override
    @Transactional
    public void deletarFuncionarioPermanentemente(UUID id) {
        if (!funcionarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Funcionário com ID " + id + " não encontrado.");
        }

        // 1. Limpa os tokens associados (por causa das Foreign Keys)
        tokenAcessoRepository.deleteAllByFuncionarioId(id);
        authCodeRepository.deleteAllByFuncionarioId(id);
        refreshTokenRepository.deleteAllByFuncionarioId(id);

        // 2. Agora o Hard Delete
        funcionarioRepository.deleteById(id);
    }


    /**
     * Lista TODOS os funcionários de TODOS os pátios (visão de Super Admin).
     * @return Lista de funcionários com dados do pátio já carregados.
     */
    @Override
    public List<Funcionario> listarTodosFuncionariosComPateo() {
        return funcionarioRepository.findAllWithPateo();
    }


    /**
     * Lista TODOS os funcionários de UM pátio específico (visão de Super Admin).
     * @param pateoId O UUID do pátio para filtrar.
     * @return Lista de funcionários daquele pátio.
     */
    @Override
    public List<Funcionario> listarTodosFuncionariosPorPateoId(UUID pateoId) {
        return funcionarioRepository.findAllByPateoIdWithPateo(pateoId);
    }


    /**
     * Método auxiliar privado para buscar um admin por ID
     */
    private UsuarioAdmin findAdminById(UUID id) {
        return usuarioAdminRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + id + " não encontrado."));
    }
}