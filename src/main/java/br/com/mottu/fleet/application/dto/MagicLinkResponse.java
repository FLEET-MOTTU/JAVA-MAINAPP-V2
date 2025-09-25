package br.com.mottu.fleet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO para a resposta de geração do Magic Link")
public record MagicLinkResponse(
    @Schema(description = "URL completa do Magic Link gerado", example = "http://localhost:8080/auth/validar-toker?valor=abc123xyz456...")
    String magicLinkUrl
) {}