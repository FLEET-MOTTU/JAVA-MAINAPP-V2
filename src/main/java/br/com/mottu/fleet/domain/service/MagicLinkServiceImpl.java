package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.api.TokenResponse;
import br.com.mottu.fleet.domain.entity.AuthCode;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.RefreshToken;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.domain.repository.AuthCodeRepository;
import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.RefreshTokenRepository;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;
import br.com.mottu.fleet.config.JwtService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class MagicLinkServiceImpl implements MagicLinkService {

    private final TokenAcessoRepository tokenAcessoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PateoRepository pateoRepository;
    private final NotificationService notificationService;
    private final AuthCodeRepository authCodeRepository;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final String baseUrl;

    public MagicLinkServiceImpl(TokenAcessoRepository tokenAcessoRepository,
                                FuncionarioRepository funcionarioRepository,
                                PateoRepository pateoRepository,
                                NotificationService notificationService,
                                AuthCodeRepository authCodeRepository,
                                JwtService jwtService,
                                RefreshTokenRepository refreshTokenRepository,
                                @Value("${application.base-url}") String baseUrl) {
        this.tokenAcessoRepository = tokenAcessoRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.pateoRepository = pateoRepository;
        this.notificationService = notificationService;
        this.authCodeRepository = authCodeRepository;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.baseUrl = baseUrl;
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
     * Valida um token de Magic Link (TokenAcesso). Se for válido, o token é invalidado (marcado como usado)
     * e um código de autorização de curta duração (AuthCode) é gerado para o funcionário.
     *
     * @param valorToken O valor do token de uso único recebido na URL do Magic Link.
     * @return A entidade AuthCode recém-criada, contendo o código de troca.
     * @throws ResourceNotFoundException se o Magic Link Token não for encontrado.
     * @throws BusinessException se o Magic Link Token já tiver sido usado ou estiver expirado.
     */
    @Override
    @Transactional
    public AuthCode validarMagicLinkEGerarAuthCode(String valorToken) {
        // valida o token de uso único
        TokenAcesso tokenAcesso = tokenAcessoRepository.findByToken(valorToken)
                .orElseThrow(() -> new ResourceNotFoundException("Magic link inválido ou não encontrado."));

        if (tokenAcesso.isUsado()) {
            throw new BusinessException("Este magic link já foi utilizado.");
        }
        if (tokenAcesso.getExpiraEm().isBefore(Instant.now())) {
            throw new BusinessException("Este magic link expirou.");
        }

        tokenAcesso.setUsado(true);
        tokenAcessoRepository.save(tokenAcesso);

        // cria e salva o código de troca de curta duração
        AuthCode authCode = new AuthCode();
        authCode.setFuncionario(tokenAcesso.getFuncionario());
        authCode.setCode(UUID.randomUUID().toString());
        authCode.setExpiraEm(Instant.now().plus(120, ChronoUnit.SECONDS));

        return authCodeRepository.save(authCode);
    }

    
    /**
     * Regenera um novo Magic Link para um funcionário existente e dispara uma notificação via WhatsApp.
     * Este método é acionado por um Administrador de Pátio autenticado.
     *
     * @param funcionarioId O UUID do funcionário que receberá o novo link.
     * @param adminLogado O UsuarioAdmin autenticado que está realizando a operação.
     * @return A URL completa do novo Magic Link gerado.
     * @throws ResourceNotFoundException se o funcionário não for encontrado.
     * @throws BusinessException se o funcionário estiver removido.
     * @throws SecurityException se o funcionário não pertencer ao pátio do admin logado.
     */
    @Override
    @Transactional
    public String regenerarLink(UUID funcionarioId, UsuarioAdmin adminLogado) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado."));

        // REGRA DE NEGÓCIO: Não é possível gerar um link para um funcionário com status REMOVIDO
        if (funcionario.getStatus() == Status.REMOVIDO) {
            throw new BusinessException("Não é possível gerar um link para um funcionário que já foi removido. Reative-o primeiro.");
        }

        // REGRA DE NEGÓCIO: Um admin só pode gerar links para funcionários do seu próprio pátio
        Pateo pateoDoAdmin = pateoRepository.findFirstByGerenciadoPorId(adminLogado.getId())
                .orElseThrow(() -> new BusinessException("Admin não está associado a nenhum pátio."));

        if (!pateoDoAdmin.getId().equals(funcionario.getPateo().getId())) {
            throw new SecurityException("Acesso negado: este funcionário não pertence ao seu pátio.");
        }

        String novoLink = this.gerarLink(funcionario);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.enviarMagicLink(funcionario, novoLink);
            }
        });
        
        return novoLink;
    }

    /**
     * Gera um novo Magic Link para um funcionário a partir de seu ID.
     * Este método é para uso interno para testes do painel de Super Admin,
     * NÃO realiza validações.
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


    /**
     * Troca um AuthCode válido por um par de tokens: um Access Token (JWT) de curta duração
     * e um Refresh Token de longa duração.
     *
     * @param code A string do código de autorização recebido do app mobile.
     * @return Um objeto TokenResponse contendo o accessToken e o refreshToken.
     * @throws BusinessException se o AuthCode for inválido, já tiver sido usado ou estiver expirado.
     */
    @Override
    @Transactional
    public TokenResponse trocarAuthCodePorTokens(String code) {
        // busca e valida o código de troca
        AuthCode authCode = authCodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Código de autorização inválido."));

        if (authCode.isUsado()) {
            throw new BusinessException("Código de autorização já foi utilizado.");
        }
        if (authCode.getExpiraEm().isBefore(Instant.now())) {
            throw new BusinessException("Código de autorização expirou.");
        }

        authCode.setUsado(true);
        authCodeRepository.save(authCode);

        Funcionario funcionario = authCode.getFuncionario();

        // gera o Access Token (JWT) de curta duração
        String accessToken = jwtService.generateToken(funcionario);

        // gera e salva o Refresh Token de longa duração
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setFuncionario(funcionario);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiraEm(Instant.now().plus(30, ChronoUnit.DAYS)); // Validade de 1 mês
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshToken.getToken());
    }


    /**
     * Valida um Refresh Token de longa duração. Se válido, gera um novo Access Token (JWT)
     * e opcionalmente rotaciona o Refresh Token.
     *
     * @param token O valor do Refresh Token enviado pelo cliente.
     * @return Um novo par de tokens (Access e Refresh).
     * @throws BusinessException se o Refresh Token for inválido ou expirado.
     */
    @Override
    @Transactional
    public TokenResponse renovarTokens(String token) {
        // busca e valida o refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Refresh Token inválido."));

        if (refreshToken.getExpiraEm().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException("Refresh Token expirou. Por favor, faça login novamente.");
        }

        Funcionario funcionario = refreshToken.getFuncionario();

        // gera o novo Access Token (JWT)
        String novoAccessToken = jwtService.generateToken(funcionario);

        // invalida o refresh token antigo e cria um novo
        refreshTokenRepository.delete(refreshToken);
        RefreshToken novoRefreshToken = new RefreshToken();
        novoRefreshToken.setFuncionario(funcionario);
        novoRefreshToken.setToken(UUID.randomUUID().toString());
        novoRefreshToken.setExpiraEm(Instant.now().plus(30, ChronoUnit.DAYS));
        refreshTokenRepository.save(novoRefreshToken);

        return new TokenResponse(novoAccessToken, novoRefreshToken.getToken());
    }
}