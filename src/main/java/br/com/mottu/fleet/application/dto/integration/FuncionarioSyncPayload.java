package br.com.mottu.fleet.application.dto.integration;

import br.com.mottu.fleet.domain.entity.Funcionario;
import java.util.UUID;

/**
 * DTO (Payload) que representa o ESTADO completo de um funcionário.
 * Usado como 'data' em todos os eventos de sincronização (Create, Update, Delete)
 * enviados para a fila de integração do C#.
 */
public record FuncionarioSyncPayload(
    UUID id,
    String nome,
    String email,
    String telefone,
    String cargo,
    String status,
    UUID pateoId,
    String fotoUrl  // <-- CAMPO ADICIONADO
) {
    /**
     * Construtor de conveniência para criar o payload a partir da entidade Funcionario.
     */
    public FuncionarioSyncPayload(Funcionario f) {
        this(
            f.getId(),
            f.getNome(),
            f.getEmail(),
            f.getTelefone(),
            f.getCargo().name(),
            f.getStatus().name(),
            f.getPateo().getId(),
            f.getFotoUrl() // <-- CAMPO ADICIONADO
        );
    }
}