package br.com.mottu.fleet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Define o algoritmo de criptografia que usaremos para as senhas.
     * O BCrypt é o padrão e o mais recomendado.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * O bean principal que configura todas as regras de segurança da aplicação.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Libera o acesso a recursos estáticos (CSS, JS)
                        .requestMatchers("/css/**", "/js/**").permitAll()
                        // Libera o acesso à página de login
                        .requestMatchers("/login").permitAll()
                        // Protege as rotas do painel admin, exigindo a role SUPER_ADMIN
                        .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                        // Qualquer outra requisição precisa de autenticação
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // Informa ao Spring qual é a nossa página de login customizada
                        .loginPage("/login")
                        // Para onde redirecionar após um login bem-sucedido
                        .defaultSuccessUrl("/admin/onboarding/novo", true)
                        // Permite que todos acessem a URL que processa o login
                        .permitAll()
                )
                .logout(logout -> logout
                        // URL para deslogar
                        .logoutUrl("/logout")
                        // Para onde ir após o logout
                        .logoutSuccessUrl("/login?logout")
                );

        return http.build();
    }
}