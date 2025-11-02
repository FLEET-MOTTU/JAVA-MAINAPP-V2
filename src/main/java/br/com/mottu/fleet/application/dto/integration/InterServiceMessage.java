package br.com.mottu.fleet.application.dto.integration;

/**
 * "Envelope" genérico para TODAS as mensagens enviadas para a fila de sincronização.
 * @param eventType O tipo de evento (ex: "FUNCIONARIO_CRIADO", "PATEO_CRIADO")
 * @param data O payload de dados (um DTO de sincronização, ex: FuncionarioSyncPayload)
 */
public record InterServiceMessage(
    String eventType,
    Object data
) {}