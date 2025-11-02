package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.domain.service.MagicLinkService;
import br.com.mottu.fleet.application.handler.GlobalExceptionHandler;
import br.com.mottu.fleet.domain.entity.AuthCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;


/**
 * Controller MVC público responsável por lidar com fluxos de autenticação
 * que iniciam em um navegador web mas se destinam a outros aplicativos,
 * como a validação do Magic Link para o app mobile.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final MagicLinkService magicLinkService;
    private final String deepLinkBaseUrl;
    private final String deepLinkSuccessPath;

    public AuthController(MagicLinkService magicLinkService,
                          @Value("${application.deeplink.base-url}") String deepLinkBaseUrl,
                          @Value("${application.deeplink.login-success-path}") String deepLinkSuccessPath) {
        this.magicLinkService = magicLinkService;
        this.deepLinkBaseUrl = deepLinkBaseUrl;
        this.deepLinkSuccessPath = deepLinkSuccessPath;
    }


    /**
     * Endpoint acionado quando o funcionário clica no Magic Link.
     * Fluxo de Execução:
     * 1. Recebe o token de uso único (`valor`).
     * 2. Chama o serviço para validar este token e gerar um AuthCode de curta duração.
     * 3. Constrói a URL do deep link do app mobile (ex: fleetapp://login-callback?code=...).
     * 4. Retorna um {@link RedirectView}, que envia uma resposta HTTP 302 para o navegador,
     * instruindo-o a abrir o aplicativo com a URL gerada.
     * Casos de erro (token inválido, expirado, etc.) são capturados pelo {@link GlobalExceptionHandler},
     * que redireciona para o deep link de erro.
     *
     * @param valor O token de uso único (UUID em String) recebido da URL do Magic Link.
     * @return Um {@code RedirectView} que aciona o deep link no dispositivo do usuário.
     */
    @GetMapping("/validar-token")
    public RedirectView validarToken(@RequestParam String valor) {
        AuthCode authCode = magicLinkService.validarMagicLinkEGerarAuthCode(valor);

        // Monta a URL de deep link para o app mobile
        String successUrl = UriComponentsBuilder.fromUriString(deepLinkBaseUrl + deepLinkSuccessPath)
                .queryParam("code", authCode.getCode())
                .toUriString();

        // Retorna um objeto que instrui o Spring a fazer um redirecionamento 302
        return new RedirectView(successUrl);
        
    }
}