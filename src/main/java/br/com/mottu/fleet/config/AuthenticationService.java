package br.com.mottu.fleet.config;

import br.com.mottu.fleet.domain.repository.UsuarioAdminRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


/**
 * Serviço de autenticação customizado que se integra ao Spring Security.
 * A única responsabilidade dessa classe é implementar a interface UserDetailsService
 * para carregar os dados do usuário (UsuarioAdmin entity) a partir do banco de dados
 * usando o email como "username".
 */
@Service
public class AuthenticationService implements UserDetailsService {

    private final UsuarioAdminRepository repository;

    public AuthenticationService(UsuarioAdminRepository repository) {
        this.repository = repository;
    }


    /**
     * Localiza um usuário no banco de dados pelo seu email (tratado como 'username' pelo Spring Security).
     * Este método é chamado automaticamente pelo AuthenticationManager do Spring
     * durante o processo de login (tanto da API quanto do painel web).
     *
     * @param username O email do usuário que está tentando se autenticar.
     * @return O objeto UserDetails (nossa entidade UsuarioAdmin) correspondente.
     * @throws UsernameNotFoundException se o usuário não for encontrado no banco de dados.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + username));
    }

}