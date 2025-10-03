package br.com.mottu.fleet.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO para a resposta de uma autenticação bem-sucedida, contendo os tokens de acesso e de atualização.")
public record TokenResponse(
    @Schema(description = "O Access Token (JWT) de curta duração, usado para autenticar as chamadas à API.",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTk5OTk5ODg4OCIsImlhdCI6MTY...")
    String accessToken,

    @Schema(description = "O Refresh Token de longa duração, usado para obter um novo Access Token quando o atual expirar.",
            example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    String refreshToken
) {}