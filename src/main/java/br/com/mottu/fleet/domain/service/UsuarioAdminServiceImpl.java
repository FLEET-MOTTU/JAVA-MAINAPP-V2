package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.repository.UsuarioAdminRepository;
import br.com.mottu.fleet.domain.repository.PateoRepository;
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


/**
 * Implementação do serviço que contém as regras de negócio para
 * o gerenciamento de usuários administradores (Super Admins e Admins de Pátio).
 */
@Service
public class UsuarioAdminServiceImpl implements UsuarioAdminService {

    private final UsuarioAdminRepository usuarioAdminRepository;
    private final PateoRepository pateoRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioAdminServiceImpl(UsuarioAdminRepository usuarioAdminRepository,
                                   PateoRepository pateoRepository,
                                   PasswordEncoder passwordEncoder) {
        this.usuarioAdminRepository = usuarioAdminRepository;
        this.pateoRepository = pateoRepository;
        this.passwordEncoder = passwordEncoder;
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
     * Método auxiliar privado para buscar um admin por ID
     */
    private UsuarioAdmin findAdminById(UUID id) {
        return usuarioAdminRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + id + " não encontrado."));
    }
}