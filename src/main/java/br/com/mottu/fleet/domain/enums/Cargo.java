package br.com.mottu.fleet.domain.enums;

/**
 * Define os cargos (funções) possíveis para um Funcionário dentro de um pátio.
 * Usado para determinar as permissões do funcionário na API (ex: ROLE_OPERACIONAL).
 */
public enum Cargo {
    OPERACIONAL,
    ADMINISTRATIVO,
    TEMPORARIO
}