package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.domain.service.MagicLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private MagicLinkService magicLinkService;

    @GetMapping("/validar-token")
    public RedirectView validarToken(@RequestParam String valor) {
        try {
            // Valida o token e gera o JWT de sessão
            String sessionToken = magicLinkService.validarTokenEGerarJwt(valor);

            // **AQUI ESTÁ A MÁGICA DO DEEP LINK**
            // Monta a URL de redirecionamento para o seu app Expo
            // O frontend precisa estar configurado para "ouvir" este scheme
            String deepLinkUrl = "exp://SEU_IP_AQUI:8081/--/login-success?token=" + sessionToken;

            return new RedirectView(deepLinkUrl);

        } catch (RuntimeException e) {
            // Se o token for inválido, redireciona para uma tela de erro no app
            String errorDeepLinkUrl = "exp://SEU_IP_AQUI:8081/--/login-error?message=" + e.getMessage();
            return new RedirectView(errorDeepLinkUrl);
        }
    }
}