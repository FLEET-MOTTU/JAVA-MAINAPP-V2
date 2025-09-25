package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;
import br.com.mottu.fleet.config.JwtService;
import br.com.mottu.fleet.domain.exception.InvalidTokenException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.domain.service.PateoService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class MagicLinkServiceImpl implements MagicLinkService {

    private final TokenAcessoRepository tokenAcessoRepository;
    private final JwtService jwtService;
    private final String baseUrl;
    private final FuncionarioRepository funcionarioRepository;
    private final PateoService pateoService;

    public MagicLinkServiceImpl(TokenAcessoRepository tokenAcessoRepository,
                                JwtService jwtService,
                                @Value("${application.base-url}") String baseUrl,
                                FuncionarioRepository funcionarioRepository,
                                PateoService pateoService) {
        this.tokenAcessoRepository = tokenAcessoRepository;
        this.jwtService = jwtService;
        this.baseUrl = baseUrl;
        this.funcionarioRepository = funcionarioRepository;
        this.pateoService = pateoService;
    }

    /**
     * Cria um novo token de acesso único para um funcionário, salva no banco e retorna a URL completa do Magic Link.
     * Regra de Negócio: O token gerado tem validade de 24 horas.
     *
     * @param funcionario O funcionário para o qual o link será gerado.
     * @return A String contendo a URL completa do Magic Link.
     */
    @Override
    public String gerarLink(Funcionario funcionario) {
        String valorToken = UUID.randomUUID().toString();

        TokenAcesso token = new TokenAcesso();
        token.setToken(valorToken);
        token.setFuncionario(funcionario);
        token.setCriadoEm(Instant.now());
        token.setExpiraEm(Instant.now().plus(24, ChronoUnit.HOURS));
        token.setUsado(false);

        tokenAcessoRepository.save(token);

        return baseUrl + "/auth/validar-token?valor=" + valorToken;
    }

    /**
     * Valida um token de acesso de uso único. Se for válido, o token é invalidado (marcado como usado)
     * e um token de sessão JWT de longa duração é gerado para o funcionário.
     *
     * @param valorToken O valor do token recebido na URL.
     * @return Uma String contendo o token JWT de sessão.
     * @throws ResourceNotFoundException se o token não for encontrado no banco de dados.
     * @throws InvalidTokenException se o token já tiver sido usado ou estiver expirado.
     */
    @Override
    @Transactional
    public String validarTokenEGerarJwt(String valorToken) {
        TokenAcesso token = tokenAcessoRepository.findByToken(valorToken)
                .orElseThrow(() -> new ResourceNotFoundException("Token de acesso não encontrado."));

        if (token.isUsado()) {
            throw new InvalidTokenException("Este link de acesso já foi utilizado.");
        }
        if (token.getExpiraEm().isBefore(Instant.now())) {
            throw new InvalidTokenException("Este link de acesso expirou.");
        }

        token.setUsado(true);
        tokenAcessoRepository.save(token);

        return jwtService.generateToken(token.getFuncionario());
    }

    @Override
    @Transactional
    public String regenerarLink(UUID funcionarioId, UsuarioAdmin adminLogado) {
        // Busca o funcionário e valida se ele existe
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado."));

        // Validação de segurança: o admin só pode gerar links para funcionários do seu pátio
        Pateo pateoDoAdmin = pateoService.buscarDetalhesDoPateo(null, adminLogado);
        if (!pateoDoAdmin.getId().equals(funcionario.getPateo().getId())) {
            throw new SecurityException("Acesso negado: você não pode gerar links para funcionários de outro pátio.");
        }

        return this.gerarLink(funcionario);
    }

    /**
     * Gera um novo Magic Link para um funcionário a partir de seu ID.
     * Este método é para uso interno ou por Super Admins, não
     * realiza validações de posse.
     *
     * @param funcionarioId O ID do funcionário.
     * @return A URL completa do novo Magic Link.
     */
    @Override
    public String gerarLink(UUID funcionarioId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário com ID " + funcionarioId + " não encontrado."));
        return this.gerarLink(funcionario);
    }
}