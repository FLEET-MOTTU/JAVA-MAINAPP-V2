package br.com.mottu.fleet.application.dto.integration;

import br.com.mottu.fleet.domain.entity.Pateo;
import java.util.UUID;

/**
 * DTO (Payload) que representa o ESTADO de um Pátio.
 * Usado em eventos de sincronização (PATEO_CRIADO, PATEO_ATUALIZADO).
 */
public record PateoSyncPayload(
    UUID id,
    String nome,
    String status,
    String plantaBaixaUrl,
    Integer plantaLargura,
    Integer plantaAltura,
    UUID gerenciadoPorId
) {
    public PateoSyncPayload(Pateo p) {
        this(
            p.getId(),
            p.getNome(),
            p.getStatus().name(),
            p.getPlantaBaixaUrl(),
            p.getPlantaLargura(),
            p.getPlantaAltura(),
            p.getGerenciadoPor().getId()
        );
    }
}