package br.com.mottu.fleet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "DTO com todos os detalhes de um pátio para a tela de desenho de zonas")
public record PateoDetailResponse(
        @Schema(description = "ID único do pátio", example = "c2a9a3f8-8a8b-4f9e-8c8d-6a5b4c3d2e1f")
        UUID id,

        @Schema(description = "Nome do pátio", example = "Pátio de Teste - Zona Leste")
        String nome,

        @Schema(description = "URL para acessar a imagem da planta baixa", example = "/images/plantas/planta-pateo-teste.png")
        String plantaBaixaUrl,

        @Schema(description = "Largura original da imagem da planta em pixels. Essencial para o cálculo de coordenadas no frontend.", example = "800")
        Integer plantaLargura,

        @Schema(description = "Altura original da imagem da planta em pixels. Essencial para o cálculo de coordenadas no frontend.", example = "724")
        Integer plantaAltura,

        @Schema(description = "Lista de todas as zonas já cadastradas para este pátio")
        List<ZonaResponse> zonas
) {}