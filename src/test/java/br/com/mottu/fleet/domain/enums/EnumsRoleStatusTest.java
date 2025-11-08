package br.com.mottu.fleet.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Testes para os enums Role e Status garantindo os valores
 */
class EnumsRoleStatusTest {

    @Test
    @DisplayName("Role contém SUPER_ADMIN e PATEO_ADMIN")
    void role_contemValoresEsperados() {
        Role[] roles = Role.values();
        assertThat(roles).contains(Role.SUPER_ADMIN, Role.PATEO_ADMIN);
    }

    @Test
    @DisplayName("Status contém ATIVO, SUSPENSO e REMOVIDO")
    void status_contemValoresEsperados() {
        Status[] statuses = Status.values();
        assertThat(statuses).contains(Status.ATIVO, Status.SUSPENSO, Status.REMOVIDO);
    }
}
