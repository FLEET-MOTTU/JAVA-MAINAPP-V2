package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;
import br.com.mottu.fleet.config.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
class MagicLinkServiceImpl implements MagicLinkService {

    @Autowired
    private TokenAcessoRepository tokenAcessoRepository;

    @Autowired 
    private JwtService jwtService; 

    @Value("${application.base-url}")
    private String baseUrl;

    @Override
    public String gerarLink(Funcionario funcionario) {
        String valorToken = UUID.randomUUID().toString();

        TokenAcesso token = new TokenAcesso();
        token.setToken(valorToken);
        token.setFuncionario(funcionario);
        token.setCriadoEm(Instant.now());
        token.setExpiraEm(Instant.now().plus(24, ChronoUnit.HOURS)); // Expira em 24 horas
        token.setUsado(false);

        tokenAcessoRepository.save(token);

        return baseUrl + "/auth/validar-token?valor=" + valorToken;
    }

    @Override
    @Transactional
    public String validarTokenEGerarJwt(String valorToken) {
        // Busca o token no banco
        TokenAcesso token = tokenAcessoRepository.findByToken(valorToken) // Precisaremos criar este método no repo
                .orElseThrow(() -> new RuntimeException("Token inválido ou não encontrado."));

        // Validações
        if (token.isUsado()) {
            throw new RuntimeException("Este link já foi utilizado.");
        }
        if (token.getExpiraEm().isBefore(Instant.now())) {
            throw new RuntimeException("Este link de acesso expirou.");
        }

        // Marca o token como usado para que não possa ser usado novamente
        token.setUsado(true);
        tokenAcessoRepository.save(token);

        // Gera o token de sessão final (JWT) para o funcionário
        return jwtService.generateToken(token.getFuncionario());
    }
    
}