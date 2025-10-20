package br.com.mottu.fleet.application.dto.integration;

/**
 * Payload genérico para todas as mensagens enviadas para a fila de sincronização.
 * @param eventType O tipo de evento (ex: "FUNCIONARIO_CRIADO", "FUNCIONARIO_ATUALIZADO")
 * @param data O payload de dados (o estado do funcionário)
 */
public record QueueMessagePayload(
    String eventType,
    FuncionarioSyncPayload data
) {}