package br.com.mottu.fleet.domain.enums;

/**
 * Define os status de ciclo de vida para entidades como UsuarioAdmin, Pateo e Funcionario.
 */
public enum Status {
    ATIVO, // Entidade está ativa e funcional
    SUSPENSO, // Entidade está temporariamente inativa (ex: férias / afastamento)
    REMOVIDO // Entidade foi removida e não deve ser mais utilizada (soft-delete)
}