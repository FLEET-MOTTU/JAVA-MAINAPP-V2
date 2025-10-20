package br.com.mottu.fleet.config.dev;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Role;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.UsuarioAdminRepository;
import br.com.mottu.fleet.domain.service.StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UsuarioAdminRepository usuarioAdminRepository;
    private final PateoRepository pateoRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;

    public DataInitializer(UsuarioAdminRepository usuarioAdminRepository,
                           PateoRepository pateoRepository,
                           PasswordEncoder passwordEncoder,
                           StorageService storageService) {
        this.usuarioAdminRepository = usuarioAdminRepository;
        this.pateoRepository = pateoRepository;
        this.passwordEncoder = passwordEncoder;
        this.storageService = storageService;
    }


    @Override
    @Transactional
    public void run(String... args) throws Exception {
        createSuperAdmin();
        createTestPateoAdminAndPateo();
    }


    private void createSuperAdmin() {
        String superAdminEmail = "super@fleet.com";

        if (usuarioAdminRepository.findByEmail(superAdminEmail).isEmpty()) {
            log.info("Nenhum Super Admin encontrado. Criando usuário padrão de desenvolvimento");

            UsuarioAdmin superAdmin = new UsuarioAdmin();
            superAdmin.setNome("Super Admin");
            superAdmin.setEmail(superAdminEmail);
            superAdmin.setSenha(passwordEncoder.encode("superadmin123"));
            superAdmin.setRole(Role.SUPER_ADMIN);
            superAdmin.setStatus(Status.ATIVO);

            usuarioAdminRepository.save(superAdmin);
            log.info("Usuário Super Admin criado com sucesso. Email: {}, Senha: superadmin123", superAdminEmail);
        } else {
            log.info("Usuário Super Admin de desenvolvimento já existe. Nenhuma ação necessária.");
        }
    }


    private void createTestPateoAdminAndPateo() {
        String pateoAdminEmail = "pateo.admin@mottu.com";
        if (usuarioAdminRepository.findByEmail(pateoAdminEmail).isEmpty()) {
            log.info("Criando usuário de teste PATEO_ADMIN e Pátio associado");

            UsuarioAdmin pateoAdmin = new UsuarioAdmin();
            pateoAdmin.setNome("Admin Pátio Teste");
            pateoAdmin.setEmail(pateoAdminEmail);
            pateoAdmin.setSenha(passwordEncoder.encode("mottu123"));
            pateoAdmin.setRole(Role.PATEO_ADMIN);
            pateoAdmin.setStatus(Status.ATIVO);
            UsuarioAdmin adminSalvo = usuarioAdminRepository.save(pateoAdmin);

            String plantaUrl = null;
            try {
                ClassPathResource resource = new ClassPathResource("static/images/plantas/planta-pateo-teste.png");
                try (InputStream inputStream = resource.getInputStream()) {
                    long length = resource.contentLength();
                    plantaUrl = storageService.upload(
                        "plantas", 
                        "planta-pateo-teste.png", 
                        inputStream, 
                        length, 
                        "image/png"
                    );
                }
            } catch (Exception e) {
                log.error("Falha ao fazer upload da planta de teste: {}", e.getMessage());
            }

            Pateo pateoDeTeste = new Pateo();
            pateoDeTeste.setNome("Pátio de Teste - Zona Leste");
            pateoDeTeste.setGerenciadoPor(adminSalvo);
            pateoDeTeste.setStatus(Status.ATIVO);
            pateoDeTeste.setPlantaBaixaUrl(plantaUrl);
            pateoDeTeste.setPlantaLargura(800);
            pateoDeTeste.setPlantaAltura(724);
            pateoRepository.save(pateoDeTeste);

            pateoRepository.save(pateoDeTeste);
            log.info("Pátio de Teste criado com sucesso. Admin: {}, Senha: mottu123", pateoAdminEmail);
        } else {
            log.info("Pátio de Teste e Admin já existem.");
        }
    }
}