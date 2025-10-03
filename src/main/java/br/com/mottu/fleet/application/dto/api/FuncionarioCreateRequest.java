package br.com.mottu.fleet.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "DTO para a criação de um novo funcionário por um Administrador de Pátio.")
public class FuncionarioCreateRequest {

    @NotBlank(message = "O nome é obrigatório")
    @Schema(description = "Nome completo do funcionário", example = "João da Silva")
    private String nome;

    @NotBlank(message = "O telefone é obrigatório")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "O telefone deve conter apenas números, com DDD (10 ou 11 dígitos)")
    @Schema(description = "Telefone do funcionário (apenas números, com DDD)", example = "11987654321")
    private String telefone;

    @NotBlank(message = "O cargo é obrigatório")
    @Pattern(regexp = "OPERACIONAL|ADMINISTRATIVO|TEMPORARIO", message = "Cargo inválido. Valores aceitos: OPERACIONAL, ADMINISTRATIVO, TEMPORARIO")
    @Schema(description = "Cargo do funcionário. Valores possíveis: OPERACIONAL, ADMINISTRATIVO, TEMPORARIO", example = "OPERACIONAL")
    private String cargo;

    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getCargo() { return cargo; }

    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setCargo(String cargo) { this.cargo = cargo; }

}