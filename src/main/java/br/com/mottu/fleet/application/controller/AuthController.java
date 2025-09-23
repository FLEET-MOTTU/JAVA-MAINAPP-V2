package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.domain.service.MagicLinkService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Controller público responsável por lidar com fluxos de autenticação que
 * não se encaixam na API REST padrão, como a validação do Magic Link.
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
     * Valida um token de Magic Link e redireciona o usuário para o deep link de sucesso.
     * Casos de erro são capturados pelo GlobalExceptionHandler.
     */
    @GetMapping("/validar-token")
    public RedirectView validarToken(@RequestParam String valor) {
        String sessionToken = magicLinkService.validarTokenEGerarJwt(valor);

        String successUrl = UriComponentsBuilder.fromUriString(deepLinkBaseUrl + deepLinkSuccessPath)
                .queryParam("token", sessionToken)
                .toUriString();

        return new RedirectView(successUrl);
    }
}