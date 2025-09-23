package br.com.mottu.fleet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para a criação de uma nova Unidade Mottu (Pátio + Administrador principal)")
public class OnboardingRequest {

    @NotBlank(message = "O nome do pátio não pode estar em branco.")
    @Schema(description = "Nome do novo pátio a ser criado", example = "Pátio Zona Sul")
    private String nomePateo;

    @NotBlank(message = "O nome do admin não pode estar em branco.")
    @Schema(description = "Nome completo do administrador principal do novo pátio", example = "Ana Pereira")
    private String nomeAdminPateo;

    @NotBlank(message = "O email do admin não pode estar em branco.")
    @Email(message = "O formato do email é inválido.")
    @Schema(description = "Email de login para o novo administrador", example = "ana.sul@mottu.com")
    private String emailAdminPateo;

    @NotBlank(message = "A senha não pode estar em branco.")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
    @Schema(description = "Senha inicial para o novo administrador (mínimo 8 caracteres)", example = "mottu#2025")
    private String senhaAdminPateo;


    public String getNomePateo() { return nomePateo; }
    public String getNomeAdminPateo() { return nomeAdminPateo; }
    public String getEmailAdminPateo() { return emailAdminPateo; }
    public String getSenhaAdminPateo() { return senhaAdminPateo; }

    public void setNomePateo(String nomePateo) { this.nomePateo = nomePateo; }
    public void setNomeAdminPateo(String nomeAdminPateo) { this.nomeAdminPateo = nomeAdminPateo; }
    public void setEmailAdminPateo(String emailAdminPateo) { this.emailAdminPateo = emailAdminPateo; }
    public void setSenhaAdminPateo(String senhaAdminPateo) { this.senhaAdminPateo = senhaAdminPateo; }

}