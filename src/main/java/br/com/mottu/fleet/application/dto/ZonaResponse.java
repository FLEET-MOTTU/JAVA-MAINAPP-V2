package br.com.mottu.fleet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "DTO para exibir os dados de uma Zona")
public record ZonaResponse(
        @Schema(description = "ID único da zona", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID id,

        @Schema(description = "Nome da zona", example = "Área de Manutenção Rápida")
        String nome,

        @Schema(description = "Coordenadas do polígono da zona em formato WKT (Well-Known Text)",
                example = "POLYGON ((0.1 0.1, 0.4 0.1, 0.4 0.4, 0.1 0.4, 0.1 0.1))")
        String coordenadasWKT
) {}