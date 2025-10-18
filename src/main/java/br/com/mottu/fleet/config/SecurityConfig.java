package br.com.mottu.fleet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


/**
 * Configuração principal de segurança da aplicação, utilizando Spring Security.
 * Define múltiplas cadeias de filtros para separar a segurança da API REST (stateless)
 * da segurança do painel web (stateful).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }


    /**
     * Define o codificador de senhas padrão para a aplicação.
     * @return Uma instância do BCryptPasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * Expõe o AuthenticationManager do Spring como um Bean para ser usado
     * nos controllers de autenticação.
     * @param config A configuração de autenticação do Spring.
     * @return O AuthenticationManager padrão.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    /**
     * Configura as regras de CORS.
     * @return A fonte de configuração do CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // TODO: Trocar "*" pela URL exata do frontend.
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    /**
     * Define a cadeia de filtros de segurança para a API REST (Stateless).
     * Esta cadeia tem prioridade 1 e se aplica a todas as rotas sob "/api/**".
     *
     * @param http O objeto HttpSecurity para configurar a segurança.
     * @return A SecurityFilterChain configurada.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable) // Desabilita CSRF, pq a API é stateless e usa JWT
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/exchange-token",
                    "/api/auth/refresh-token"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }


    /**
     * Define a cadeia de filtros de segurança para o Painel Web (Stateful).
     * Esta cadeia tem prioridade 2 e se aplica a todas as outras rotas.
     *
     * @param http O objeto HttpSecurity para configurar a segurança.
     * @return A SecurityFilterChain configurada.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                // Desabilita o CSRF especificamente para a rota de webhooks
                .ignoringRequestMatchers("/ws/**")
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/error").permitAll()
                .requestMatchers("/login", "/auth/validar-token", "/ws/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()
                .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/admin/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
            );

        return http.build();
    }
}