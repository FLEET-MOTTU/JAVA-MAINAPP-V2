package br.com.mottu.fleet.application.dto.web;

import br.com.mottu.fleet.domain.entity.UsuarioAdmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;


/**
 * DTO para a atualização dos dados de um Administrador de Pátio
 * a partir do painel do Super Admin.
 */
public class UsuarioAdminUpdateRequest {

    private UUID id;

    @NotBlank(message = "O novo nome não pode estar em branco.")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
    private String nome;

    private String email; // Readonly

    @Pattern(regexp = "^$|.{8,}", message = "A nova senha deve ter no mínimo 8 caracteres, se fornecida.")
    private String newPassword; // Opcional

    /**
     * Factory para criar a DTO usando a entidade UsuarioAdmin.
     * @param usuario A entidade vinda do banco.
     * @return Uma instância de UsuarioAdminUpdateRequest preenchida.
     */
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
    public String getNewPassword() { return newPassword; }

    public void setId(UUID id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}