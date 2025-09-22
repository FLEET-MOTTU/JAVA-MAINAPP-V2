package br.com.mottu.fleet.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class UsuarioAdminUpdateRequest {

    private UUID id;

    @NotBlank(message = "O novo nome não pode estar em branco.")
    private String nome;
    
    private String email;

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }

    public void setId(UUID id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }

}