package br.com.mottu.fleet.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO para a resposta de uma autenticação bem-sucedida.")
public record AuthenticationResponse(
    @Schema(description = "Token JWT gerado para a sessão do usuário", 
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBlbWFpbC5jb20iLCJpYXQiOjE2...")
    String token
) {}