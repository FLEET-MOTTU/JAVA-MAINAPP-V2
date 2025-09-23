package br.com.mottu.fleet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "DTO para a requisição de autenticação via email e senha.")
public record AuthenticationRequest(

    @NotBlank(message = "Email não pode ser em branco")
    @Email(message = "Formato de email inválido")
    @Schema(description = "Email de login do Administrador do Pátio", example = "pateo.admin@mottu.com")
    String email,

    @NotBlank(message = "Senha não pode estar em branco")
    @Schema(description = "Senha de login do Administrador do Pátio", example = "mottu123")
    String senha
) {}