package br.com.mottu.fleet;

import org.junit.jupiter.api.Test;

/**
 * Teste que não carrega o contexto Spring pra garantir
 * que a suíte de testes tem pelo menos um teste sem dependências externas.
 */
class SmokeAppTest {

    @Test
    void testeBasicoSemContexto() {
    }
}
