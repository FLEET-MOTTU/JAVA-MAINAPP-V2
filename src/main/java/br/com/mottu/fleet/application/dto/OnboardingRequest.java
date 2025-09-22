package br.com.mottu.fleet.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OnboardingRequest {

    @NotBlank(message = "O nome do pátio não pode estar em branco.")
    private String nomePateo;

    @NotBlank(message = "O nome do admin não pode estar em branco.")
    private String nomeAdminPateo;

    @NotBlank(message = "O email do admin não pode estar em branco.")
    @Email(message = "O formato do email é inválido.")    
    private String emailAdminPateo;

    @NotBlank(message = "A senha não pode estar em branco.")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")    
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