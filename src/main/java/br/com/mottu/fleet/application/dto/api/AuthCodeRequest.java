package br.com.mottu.fleet.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "DTO para a requisição de troca de um código de autorização por tokens.")
public record AuthCodeRequest(
    @NotBlank(message = "O código de autorização é obrigatório.")
    @Schema(description = "O código de troca de curta duração recebido via deep link.", 
            example = "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d")
    String code
) {}