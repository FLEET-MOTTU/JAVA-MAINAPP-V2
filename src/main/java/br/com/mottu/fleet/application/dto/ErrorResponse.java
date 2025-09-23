package br.com.mottu.fleet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "DTO padrão para respostas de erro da API")
public record ErrorResponse(
    @Schema(description = "Timestamp de quando o erro ocorreu", example = "2025-09-23T03:11:55.123Z")
    Instant timestamp,

    @Schema(description = "Código de status HTTP", example = "404")
    int status,

    @Schema(description = "Tipo do erro HTTP", example = "Not Found")
    String error,

    @Schema(description = "Mensagem detalhada do erro", example = "Recurso com ID x não encontrado.")
    String message,

    @Schema(description = "Caminho da URI onde o erro ocorreu", example = "/api/funcionarios/x")
    String path
) {}