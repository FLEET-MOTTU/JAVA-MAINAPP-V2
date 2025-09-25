package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.repository.UsuarioAdminRepository;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.application.dto.OnboardingRequest;
import br.com.mottu.fleet.application.dto.UsuarioAdminUpdateRequest;
import br.com.mottu.fleet.domain.enums.Role;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.EmailAlreadyExistsException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.application.dto.PasswordChangeRequest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
public class UsuarioAdminServiceImpl implements UsuarioAdminService {

    private final UsuarioAdminRepository usuarioAdminRepository;
    private final PateoRepository pateoRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioAdminServiceImpl(UsuarioAdminRepository usuarioAdminRepository, PateoRepository pateoRepository, PasswordEncoder passwordEncoder) {
        this.usuarioAdminRepository = usuarioAdminRepository;
        this.pateoRepository = pateoRepository;
        this.passwordEncoder = passwordEncoder;
    }
    

    @Override
    public List<UsuarioAdmin> listarAdminsDePateo() {
        return usuarioAdminRepository.findAllByRoleAndStatus(Role.PATEO_ADMIN, Status.ATIVO);
    }


    /**
     * Realiza o "soft delete" de um administrador e, como consequência, de todos os pátios 
     * que ele gerencia.
     * Regra de Negócio: A remoção de um admin implica na remoção lógica de suas unidades.
     * O status de ambos os registros (admin e pátios) é alterado para REMOVIDO.
     *
     * @param id O UUID do administrador a ser removido.
     */
    @Override
    @Transactional
    public void desativarPorId(UUID id) {
        UsuarioAdmin admin = usuarioAdminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + id + " não encontrado."));

        admin.setStatus(Status.REMOVIDO);
        usuarioAdminRepository.save(admin);

        List<Pateo> pateos = pateoRepository.findAllByGerenciadoPorId(id);

        pateos.forEach(pateo -> pateo.setStatus(Status.REMOVIDO));
        pateoRepository.saveAll(pateos);
    }


    @Override
    public Optional<UsuarioAdmin> buscarPorId(UUID id) {
        return usuarioAdminRepository.findById(id);
    }


    @Override
    @Transactional
    public void atualizar(UsuarioAdminUpdateRequest request) {
        UsuarioAdmin adminExistente = usuarioAdminRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com ID " + request.getId() + " não encontrado para atualização."));

        adminExistente.setNome(request.getNome());
        usuarioAdminRepository.save(adminExistente);
    }

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

    @Override
    @Transactional
    public void alterarSenha(UsuarioAdmin adminLogado, PasswordChangeRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), adminLogado.getPassword())) {
            throw new BusinessException("A senha atual está incorreta.");
        }
        adminLogado.setSenha(passwordEncoder.encode(request.newPassword()));
        usuarioAdminRepository.save(adminLogado);
    }
}