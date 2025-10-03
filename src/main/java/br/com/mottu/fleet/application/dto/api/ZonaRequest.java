package br.com.mottu.fleet.application.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "DTO para a criação ou atualização de uma Zona em um Pátio")
public record ZonaRequest(
    @NotBlank(message = "O nome da zona é obrigatório")
    @Schema(description = "Nome descritivo da zona", example = "Área de Reparos Leves")
    String nome,

    @NotBlank(message = "As coordenadas são obrigatórias")
    @Schema(description = "Coordenadas do polígono da zona em formato WKT (Well-Known Text)",
            example = "POLYGON ((0.1 0.1, 0.4 0.1, 0.4 0.4, 0.1 0.4, 0.1 0.1))")
    String coordenadasWKT
) {}