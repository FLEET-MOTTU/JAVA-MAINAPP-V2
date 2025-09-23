package br.com.mottu.fleet.application.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    /**
     * Retorna o nome do arquivo HTML que deve ser
     * renderizado quando um usuário acessa a URL "/login" via GET.
     * @return O nome da view "login" pro Thymeleaf.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}