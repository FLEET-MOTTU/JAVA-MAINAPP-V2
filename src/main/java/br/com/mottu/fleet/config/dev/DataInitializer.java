package br.com.mottu.fleet.config.dev;

import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.repository.UsuarioAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UsuarioAdminRepository usuarioAdminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String superAdminEmail = "super@fleet.com";

        if (usuarioAdminRepository.findByEmail(superAdminEmail).isEmpty()) {
            log.info("Nenhum Super Admin encontrado. Criando usuário padrão de desenvolvimento...");

            UsuarioAdmin superAdmin = new UsuarioAdmin();
            superAdmin.setNome("Super Admin");
            superAdmin.setEmail(superAdminEmail);
            superAdmin.setSenha(passwordEncoder.encode("superadmin123"));
            superAdmin.setRole("SUPER_ADMIN");
            superAdmin.setStatus("ATIVO");

            usuarioAdminRepository.save(superAdmin);
            log.info("Usuário Super Admin criado com sucesso. Email: {}, Senha: superadmin123", superAdminEmail);
        } else {
            log.info("Usuário Super Admin de desenvolvimento já existe. Nenhuma ação necessária.");
        }
    }
}