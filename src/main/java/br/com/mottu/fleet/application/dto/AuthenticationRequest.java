package br.com.mottu.fleet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthenticationRequest {

    @NotBlank(message = "Email não pode ser em branco")
    @Email(message = "Formato de email inválido")
    @Schema(description = "Email de login do Administrador do Pátio", example = "adminpateo@mottu.com")
    private String email;

    @NotBlank(message = "Senha não pode estar em branco")
    @Schema(description = "Senha de login do Administrador do Pátio", example = "senha123")
    private String senha;


    public String getEmail() { return email; }
    public String getSenha() { return senha; }

    public void setEmail(String email) { this.email = email; }
    public void setSenha(String senha) { this.senha = senha; }

}