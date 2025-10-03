package br.com.mottu.fleet.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "DTO para a resposta de operações com funcionários, como criação e atualização.")
public class FuncionarioResponse {

    @Schema(description = "ID único do funcionário criado", example = "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d")
    private UUID id;

    @Schema(description = "Nome do funcionário", example = "Funcionário Teste")
    private String nome;

    @Schema(description = "Telefone do funcionário", example = "11999998888")
    private String telefone;

    @Schema(description = "URL do Magic Link para o primeiro acesso do funcionário. Retornado apenas na criação.", 
            nullable = true, example = "http://localhost:8080/auth/validar-token?valor=5e7da6e7-516e-4e2e-ba0b-48ff94678828")
    private String magicLinkUrl;

    public FuncionarioResponse() {}

    public FuncionarioResponse(UUID id, String nome, String telefone, String magicLinkUrl) {
        this.id = id;
        this.nome = nome;
        this.telefone = telefone;
        this.magicLinkUrl = magicLinkUrl;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getMagicLinkUrl() { return magicLinkUrl; }

    public void setId(UUID id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setMagicLinkUrl(String magicLinkUrl) { this.magicLinkUrl = magicLinkUrl; }

}