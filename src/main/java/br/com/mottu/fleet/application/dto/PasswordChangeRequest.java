package br.com.mottu.fleet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para a requisição de mudança de senha de um usuário.")
public record PasswordChangeRequest(
    @NotBlank(message = "A senha atual é obrigatória.")
    @Schema(description = "Senha atual do usuário para verificação", example = "mottu123")
    String currentPassword,

    @NotBlank(message = "A nova senha é obrigatória.")
    @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres.")
    @Schema(description = "Nova senha desejada (mínimo 8 caracteres)", example = "Mottu@2025!")
    String newPassword
) {}