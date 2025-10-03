package br.com.mottu.fleet.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "DTO para a atualização dos dados de um funcionário existente.")
public class FuncionarioUpdateRequest {

    @NotBlank(message = "O nome é obrigatório")
    @Schema(description = "Nome completo do funcionário", example = "João da Silva Santos")
    private String nome;

    @NotBlank(message = "O telefone é obrigatório")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "O telefone deve conter apenas números, com DDD (10 ou 11 dígitos)")
    @Schema(description = "Telefone do funcionário (apenas números, com DDD)", example = "11987654321")
    private String telefone;

    @NotBlank(message = "O cargo é obrigatório")
    @Pattern(regexp = "OPERACIONAL|ADMINISTRATIVO|TEMPORARIO", message = "Cargo inválido. Valores aceitos: OPERACIONAL, ADMINISTRATIVO, TEMPORARIO")
    @Schema(description = "Cargo do funcionário. Valores possíveis: OPERACIONAL, ADMINISTRATIVO, TEMPORARIO", example = "OPERACIONAL")
    private String cargo;

    @NotBlank(message = "O status é obrigatório")
    @Pattern(regexp = "ATIVO|SUSPENSO", message = "Status inválido. Para remover, use o endpoint DELETE. Valores aceitos: ATIVO, SUSPENSO")
    @Schema(description = "Status do funcionário. Valores possíveis: ATIVO, SUSPENSO", example = "ATIVO")
    private String status;
    
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getCargo() { return cargo; }
    public String getStatus() { return status; }

    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public void setStatus(String status) { this.status = status; }

}
