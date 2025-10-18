package br.com.mottu.fleet.config;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Role;
import br.com.mottu.fleet.domain.repository.PateoRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


/**
 * Serviço responsável por todas as operações relacionadas a JWT.
 * Geração, validação e extração de claims dos tokens.
 */
@Service
public class JwtService {

    private final String secretKey;
    private final long jwtExpiration;
    private final PateoRepository pateoRepository;

    public JwtService(@Value("${application.security.jwt.secret-key}") String secretKey,
                      @Value("${application.security.jwt.expiration}") long jwtExpiration,
                      PateoRepository pateoRepository) {
        this.secretKey = secretKey;
        this.jwtExpiration = jwtExpiration;
        this.pateoRepository = pateoRepository;
    }


    /**
     * Extrai o "subject" do token.
     * @param token O token JWT.
     * @return O subject do token como uma String.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    /**
     * Gera um token para um usuário, adicionando claims customizados.
     * @param userDetails O objeto UserDetails do usuário autenticado.
     * @return Uma String contendo o token JWT compactado.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }


    /**
     * Lógica principal de geração de token. Adiciona claims extras com base no tipo de usuário
     * (UsuarioAdmin ou Funcionario) para colocar no token com informações úteis.
     * @param extraClaims Um mapa para claims adicionais.
     * @param userDetails O objeto UserDetails do usuário.
     * @return O token JWT final.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {

        // Adiciona as claims
        if (userDetails instanceof UsuarioAdmin admin) {
            extraClaims.put("nome", admin.getNome());
            extraClaims.put("role", admin.getRole().name());

            // Regra de Negócio: Se for um admin de pátio, adiciona o ID do pátio ao token.
            if (Role.PATEO_ADMIN.equals(admin.getRole())) {
                pateoRepository.findFirstByGerenciadoPorId(admin.getId())
                        .ifPresent(pateo -> extraClaims.put("pateoId", pateo.getId()));
            }

        } else if (userDetails instanceof Funcionario funcionario) {
            extraClaims.put("nome", funcionario.getNome());
            extraClaims.put("role", funcionario.getCargo().name()); // cargo = enum Role
            if (funcionario.getPateo() != null) {
                extraClaims.put("pateoId", funcionario.getPateo().getId());
            }
        }

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }


    /**
     * Valida se um token é válido.
     * Checa se o username no token corresponde e se o token não expirou.
     *
     * @param token O token JWT a ser validado.
     * @param userDetails Opcional. O username do token é comparado com o do userDetails.
     * @return true se o token for válido, false caso contrário.
     */
    public boolean isTokenValid(String token, @Nullable UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean isUsernameValid = (userDetails == null) || username.equals(userDetails.getUsername());
            return isUsernameValid && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Método para extrair qualquer as claims de um token.
     * @param token O token JWT.
     * @param claimsResolver Uma função que define qual claim extrair.
     * @return O valor do claim.
     */
    public <T> T extractClaim(String token, @NonNull Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    /**
     * Método auxiliar que extrai todas as claims de um token.
     * Ele valida a assinatura do token no processo.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    /**
     * Método auxiliar que verifica se um token expirou.
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }


    /**
     * Método auxiliar que gera a chave de assinatura a partir da secret key em texto.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
}