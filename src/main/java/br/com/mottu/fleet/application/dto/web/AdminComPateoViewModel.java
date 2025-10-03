package br.com.mottu.fleet.application.dto.web;

import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "ViewModel para exibir um Administrador de Pátio junto com o nome do pátio que ele gerencia. Usado na tela de listagem do Super Admin.")
public record AdminComPateoViewModel(
    @Schema(description = "O objeto completo da entidade UsuarioAdmin.")
    UsuarioAdmin admin,

    @Schema(description = "O nome do pátio que este administrador gerencia.", example = "Pátio Zona Leste")
    String nomePateo
) {}