package br.com.mottu.fleet.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 * Filtro de segurança customizado que intercepta todas as requisições à API (uma vez por requisição).
 * A principal responsabilidade deste filtro é validar o token JWT (Bearer Token)
 * e configurar o Contexto de Segurança do Spring (SecurityContextHolder)
 * com a identidade do usuário autenticado.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * @param jwtService O serviço responsável por manipular os tokens JWT.
     * @param userDetailsService O serviço do Spring Security para carregar os dados do usuário.
     */
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }


    /**
     * Lógica principal do filtro.
     * Extrai o token do cabeçalho de autorização, valida e, se for válido,
     * autentica o usuário para o restante da cadeia de filtros da requisição.
     *
     * @param request A requisição HTTP recebida.
     * @param response A resposta HTTP.
     * @param filterChain A cadeia de filtros de segurança.
     * @throws ServletException Se ocorrer um erro de servlet.
     * @throws IOException Se ocorrer um erro de I/O.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Se não houver token ou não for um Bearer token, passa a requisição para o próximo filtro.
        // Permite o acesso a endpoints públicos (como /api/auth/login).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String jwt = authHeader.substring(7);
        final String userEmail = jwtService.extractUsername(jwt);

        // 2. Se o token contém um email E o usuário ainda não está autenticado nesta requisição
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 3. Se o token for válido (assinatura correta, não expirado, e bate com o usuário)
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // Senha nula pois a API usa JWT
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 4. Passa a requisição (autenticada ou não) para o próximo filtro na cadeia.
        filterChain.doFilter(request, response);
    }
    
}