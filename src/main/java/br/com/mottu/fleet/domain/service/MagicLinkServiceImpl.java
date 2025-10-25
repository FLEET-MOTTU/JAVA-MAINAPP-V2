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


/**
 * Implementação do serviço de domínio que gerencia todo o ciclo de vida
 * da autenticação por Magic Link, desde a criação do link até a
 * troca final por tokens de acesso.
 */
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
     * Cria um novo token de acesso único (TokenAcesso) para um funcionário e o salva no banco.
     * Este é o método base para gerar qualquer Magic Link.
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

        // 1. Busca e valida o Magic Link
        TokenAcesso tokenAcesso = tokenAcessoRepository.findByToken(valorToken)
                .orElseThrow(() -> new ResourceNotFoundException("Magic link inválido ou não encontrado."));

        // 2. Valida se expirou (pelo tempo de criação)
        if (tokenAcesso.getExpiraEm().isBefore(Instant.now())) {
            throw new BusinessException("Este magic link expirou.");
        }

        if (tokenAcesso.isUsado()) {
            Instant usadoEm = tokenAcesso.getUsadoEm();
            Instant agora = Instant.now();

            if (usadoEm != null && usadoEm.plus(5, ChronoUnit.MINUTES).isBefore(agora)) {
                throw new BusinessException("Este magic link já foi utilizado.");
            }
        }

        // 3. Invalida o Magic Link
        tokenAcesso.setUsado(true);
        tokenAcesso.setUsadoEm(Instant.now());
        tokenAcessoRepository.save(tokenAcesso);

        // 3. Cria o código de troca de 2 minutos (AuthCode)
        AuthCode authCode = new AuthCode();
        authCode.setFuncionario(tokenAcesso.getFuncionario());
        authCode.setCode(UUID.randomUUID().toString());
        authCode.setExpiraEm(Instant.now().plus(120, ChronoUnit.SECONDS)); // 2 minutos de validade

        return authCodeRepository.save(authCode);
    }

    
    /**
     * Regenera um novo Magic Link para um funcionário existente e agenda uma notificação.
     * Este método é acionado por um Administrador de Pátio e contém validações de segurança.
     *
     * @param funcionarioId O UUID do funcionário que receberá o novo link.
     * @param adminLogado O UsuarioAdmin autenticado que está realizando a operação.
     * @return A URL completa do novo Magic Link gerado.
     * @throws ResourceNotFoundException se o funcionário não for encontrado.
     * @throws BusinessException se o funcionário estiver removido ou o admin não tiver pátio.
     * @throws SecurityException se o funcionário não pertencer ao pátio do admin logado.
     */
    @Override
    @Transactional
    public String regenerarLink(UUID funcionarioId, UsuarioAdmin adminLogado) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado."));

        // Regra 1: Não gerar link para funcionário removido
        if (funcionario.getStatus() == Status.REMOVIDO) {
            throw new BusinessException("Não é possível gerar um link para um funcionário que já foi removido. Reative-o primeiro.");
        }

        // Regra 2: Validação de segurança (Admin só pode gerar link para seu próprio pátio)        
        Pateo pateoDoAdmin = pateoRepository.findFirstByGerenciadoPorId(adminLogado.getId())
                .orElseThrow(() -> new BusinessException("Admin não está associado a nenhum pátio."));

        if (!pateoDoAdmin.getId().equals(funcionario.getPateo().getId())) {
            throw new SecurityException("Acesso negado: este funcionário não pertence ao seu pátio.");
        }

        // Reutiliza o método base para criar o token e a URL
        String novoLink = this.gerarLink(funcionario);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Chama o serviço @Primary (Twilio WhatsApp)
                notificationService.enviarMagicLink(funcionario, novoLink);
            }
        });
        
        return novoLink;
    }


    /**
     * Gera um novo Magic Link para um funcionário (usado pelo Super Admin).
     * Este método é para uso interno (testes do painel), NÃO realiza validações
     * de segurança de pátio ou status.
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
        // 1. Busca e valida o código de troca
        AuthCode authCode = authCodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Código de autorização inválido."));

        if (authCode.isUsado()) {
            throw new BusinessException("Código de autorização já foi utilizado.");
        }
        if (authCode.getExpiraEm().isBefore(Instant.now())) {
            throw new BusinessException("Código de autorização expirou.");
        }

        // 2. Invalida (queima) o AuthCode
        authCode.setUsado(true);
        authCodeRepository.save(authCode);

        Funcionario funcionario = authCode.getFuncionario();

        // 3. Gera o Access Token (JWT)
        String accessToken = jwtService.generateToken(funcionario);

        // 4. Gera e salva o Refresh Token de longa duração
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setFuncionario(funcionario);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiraEm(Instant.now().plus(30, ChronoUnit.DAYS)); // 30 dias
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshToken.getToken());
    }


    /**
     * Valida um Refresh Token. Se válido, gera um novo Access Token (JWT)
     * e rotaciona o Refresh Token (cria um novo e invalida o antigo).
     *
     * @param token O valor do Refresh Token enviado pelo cliente.
     * @return Um novo par de tokens (Access e Refresh).
     * @throws BusinessException se o Refresh Token for inválido ou expirado.
     */
    @Override
    @Transactional
    public TokenResponse renovarTokens(String token) {

        // 1. Busca e valida o refresh token
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Refresh Token inválido."));

        if (refreshToken.getExpiraEm().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken); // Limpa o token expirado
            throw new BusinessException("Refresh Token expirou. Por favor, faça login novamente.");
        }

        Funcionario funcionario = refreshToken.getFuncionario();

        // 2. Gera o novo Access Token (JWT)
        String novoAccessToken = jwtService.generateToken(funcionario);

        // 3. ROTACIONA O REFRESH TOKEN (por segurança)
        // Invalida o token antigo e cria um novo.
        refreshTokenRepository.delete(refreshToken);
        RefreshToken novoRefreshToken = new RefreshToken();
        novoRefreshToken.setFuncionario(funcionario);
        novoRefreshToken.setToken(UUID.randomUUID().toString());
        novoRefreshToken.setExpiraEm(Instant.now().plus(30, ChronoUnit.DAYS));
        refreshTokenRepository.save(novoRefreshToken);

        return new TokenResponse(novoAccessToken, novoRefreshToken.getToken());
    }
}