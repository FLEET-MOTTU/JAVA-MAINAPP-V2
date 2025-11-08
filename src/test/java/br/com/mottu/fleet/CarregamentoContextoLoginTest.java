package br.com.mottu.fleet;

import br.com.mottu.fleet.application.controller.LoginController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Teste que carrega apenas o bean {@link LoginController} para verificar
 * que o controlador está disponível no contexto de teste.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {LoginController.class})
public class CarregamentoContextoLoginTest {

    @Autowired
    private LoginController loginController;

    @Test
    void contextoCarrega_eLoginControllerPresente() {
        assertThat(loginController).isNotNull();
    }
}
