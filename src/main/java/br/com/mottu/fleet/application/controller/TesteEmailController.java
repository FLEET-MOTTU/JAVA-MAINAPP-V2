package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.service.NotificationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TesteEmailController {

    private final NotificationService emailService;

    public TesteEmailController(@Qualifier("sendGridEmailNotificationServiceImpl") NotificationService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/teste-email")
    public String testarEmail() {
        Funcionario f = new Funcionario();
        f.setId(java.util.UUID.randomUUID());
        f.setNome("Usuario de Teste");
        f.setEmail("ajourneya4@gmail.com"); // Use seu e-mail

        emailService.enviarMagicLink(f, "http://link.teste.com");
        return "Tentativa de envio de e-mail de teste realizada. Verifique os logs do backend.";
    }
}