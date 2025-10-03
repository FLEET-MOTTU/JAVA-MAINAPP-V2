package br.com.mottu.fleet.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "DTO para a requisição de renovação de token de acesso.")
public record RefreshTokenRequest(
    @NotBlank(message = "O refresh token é obrigatório.")
    @Schema(description = "O Refresh Token de longa duração obtido durante o login.", 
            example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    String refreshToken
) {}