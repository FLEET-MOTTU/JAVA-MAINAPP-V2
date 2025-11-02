package br.com.mottu.fleet.application.dto.integration;

import br.com.mottu.fleet.domain.entity.Zona;
import org.locationtech.jts.io.WKTWriter;
import java.util.UUID;

/**
 * DTO (Payload) que representa o ESTADO de uma Zona.
 * Usado em eventos de sincronização (ZONA_CRIADA, ZONA_ATUALIZADA).
 */
public record ZonaSyncPayload(
    UUID id,
    String nome,
    UUID pateoId,
    UUID criadoPorId,
    String coordenadasWKT
) {
    // Usamos um WKTWriter estático para converter o Polygon
    private static final WKTWriter writer = new WKTWriter();

    public ZonaSyncPayload(Zona z) {
        this(
            z.getId(),
            z.getNome(),
            z.getPateo().getId(),
            z.getCriadoPor().getId(),
            writer.write(z.getCoordenadas()) // Converte Polygon para String WKT
        );
    }
}