package br.com.mottu.fleet.application.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Testes MVC para o {@link LoginController} usando MockMvc.
 * O ViewResolver é substituído por uma implementação no-op para evitar
 * renderização real nas execuções de teste.
 */
class LoginControllerMvcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        LoginController controller = new LoginController();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers((viewName, locale) -> new View() {
                    @Override
                    public String getContentType() {
                        return "text/html";
                    }

                    @Override
                    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
                    }
                })
                .build();
    }

    @Test
    @DisplayName("GET /login deve retornar 200 OK")
    void getLogin_retornaStatusOk() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }
}
