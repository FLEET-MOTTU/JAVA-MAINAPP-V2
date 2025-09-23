package br.com.mottu.fleet.application.dto;

import br.com.mottu.fleet.domain.entity.UsuarioAdmin;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "DTO para a atualização dos dados de um Administrador de Pátio")
public class UsuarioAdminUpdateRequest {

    @Schema(description = "ID do administrador que será atualizado", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @NotBlank(message = "O novo nome não pode estar em branco.")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
    @Schema(description = "Novo nome do administrador", example = "Carlos Souza")
    private String nome;

    @Schema(description = "Email do administrador (Apenas para exibição, não pode ser alterado)",
            example = "adminpateo@mottu.com", accessMode = Schema.AccessMode.READ_ONLY)
    private String email;

    public static UsuarioAdminUpdateRequest fromEntity(UsuarioAdmin usuario) {
        UsuarioAdminUpdateRequest request = new UsuarioAdminUpdateRequest();
        request.setId(usuario.getId());
        request.setNome(usuario.getNome());
        request.setEmail(usuario.getEmail());
        return request;
    }


    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }

    public void setId(UUID id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }

}