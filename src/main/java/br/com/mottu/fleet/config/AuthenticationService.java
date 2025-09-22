package br.com.mottu.fleet.config;

import br.com.mottu.fleet.domain.repository.UsuarioAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService implements UserDetailsService {

    @Autowired
    private UsuarioAdminRepository repository;

    /**
     * Este método é chamado pelo Spring Security toda vez que um usuário
     * tenta se autenticar.
     * @param username O "username" que o usuário digitou no formulário (no nosso caso, o email).
     * @return O UserDetails (nossa classe UsuarioAdmin) encontrado no banco.
     * @throws UsernameNotFoundException se o usuário não for encontrado.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + username));
    }
}