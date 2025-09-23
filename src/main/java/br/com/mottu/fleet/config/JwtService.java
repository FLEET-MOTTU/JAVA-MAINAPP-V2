package br.com.mottu.fleet.config;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Role;
import br.com.mottu.fleet.domain.repository.PateoRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


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
     * Extrai o subject do token JWT.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Gera um token JWT para um usuário, sem claims extras.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Gera um token JWT para um usuário, adicionando claims customizados com base no tipo de usuário.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        if (userDetails instanceof UsuarioAdmin admin) {
            extraClaims.put("nome", admin.getNome());
            extraClaims.put("role", admin.getRole().name()); // Converte o Enum para String

            // Regra de Negócio: Se for um admin de pátio, adiciona o ID do pátio ao token.
            if (Role.PATEO_ADMIN.equals(admin.getRole())) {
                pateoRepository.findAllByGerenciadoPorId(admin.getId())
                        .stream().findFirst()
                        .ifPresent(pateo -> extraClaims.put("pateoId", pateo.getId()));
            }
        } else if (userDetails instanceof Funcionario funcionario) {
            extraClaims.put("nome", funcionario.getNome());
            extraClaims.put("role", funcionario.getCargo().name());
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
     * Valida se um token é válido para um determinado usuário.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}