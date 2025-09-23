package br.com.mottu.fleet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public class FuncionarioResponse {

    @Schema(description = "ID único do funcionário criado")
    private UUID id;

    @Schema(description = "Nome do funcionário")
    private String nome;

    @Schema(description = "Telefone do funcionário")
    private String telefone;

    @Schema(description = "URL do Magic Link para o primeiro acesso do funcionário")
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