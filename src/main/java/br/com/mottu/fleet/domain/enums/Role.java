package br.com.mottu.fleet.domain.enums;

/**
 * Define as "Roles" (funções) de administração do sistema.
 * Usado pelo Spring Security para controlar o acesso aos endpoints da API e do painel web.
 */
public enum Role {
    SUPER_ADMIN, // Acesso total ao painel web /admin
    PATEO_ADMIN // Acesso total à API REST /api
}
