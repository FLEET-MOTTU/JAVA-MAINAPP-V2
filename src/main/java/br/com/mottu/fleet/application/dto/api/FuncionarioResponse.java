package br.com.mottu.fleet.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "DTO para exibir os dados de um funcionário.")
public record FuncionarioResponse(
    @Schema(description = "ID único do funcionário", example = "a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d")
    UUID id,

    @Schema(description = "Nome do funcionário", example = "Funcionário Teste")
    String nome,

    @Schema(description = "Telefone do funcionário", example = "11999998888")
    String telefone,

    @Schema(description = "Email do funcionário", example = "email@funcionario.com")
    String email
) {}