package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.OnboardingRequest;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.UsuarioAdminRepository;
import br.com.mottu.fleet.domain.exception.EmailAlreadyExistsException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OnboardingServiceImpl  implements OnboardingService {

    @Autowired
    private UsuarioAdminRepository usuarioAdminRepository;

    @Autowired
    private PateoRepository pateoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Cadastro de uma nova unidade e administrador.
    @Override
    @Transactional
    public void executar(OnboardingRequest request) {

        usuarioAdminRepository.findByEmail(request.getEmailAdminPateo())
                .ifPresent(usuario -> {
                    throw new EmailAlreadyExistsException("O email informado já está em uso.");
                });
                
        UsuarioAdmin novoAdminPateo = new UsuarioAdmin();
        novoAdminPateo.setNome(request.getNomeAdminPateo());
        novoAdminPateo.setEmail(request.getEmailAdminPateo());
        novoAdminPateo.setSenha(passwordEncoder.encode(request.getSenhaAdminPateo()));
        novoAdminPateo.setRole("PATEO_ADMIN");
        novoAdminPateo.setStatus("ATIVO");

        UsuarioAdmin adminSalvo = usuarioAdminRepository.save(novoAdminPateo);

        Pateo novoPateo = new Pateo();
        novoPateo.setNome(request.getNomePateo());
        novoPateo.setGerenciadoPor(adminSalvo);
        novoPateo.setStatus("ATIVO");

        pateoRepository.save(novoPateo);
    }
}