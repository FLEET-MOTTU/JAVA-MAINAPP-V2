package br.com.mottu.fleet.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


/**
 * Configuração centralizada do OpenAPI (Springdoc) para a documentação do Swagger UI.
 * Define as informações globais da API, o esquema de segurança JWT
 * e customizações para tipos de dados e URLs de servidor.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Mottu F.L.E.E.T. API",
        version = "v2",
        description = "API responsável pelo gerenciamento de pátios, administradores e funcionários do sistema FLEET."
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)

public class SwaggerConfig {

    /**
     * Ensina o Swagger a exibir o tipo 'Polygon' da biblioteca JTS
     * como uma String no formato WKT, com uma descrição e um exemplo claros.
     *
     * @return Um customizador do OpenAPI.
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            Schema<?> polygonSchema = new StringSchema()
                    .description("Representação de um polígono em formato WKT (Well-Known Text)")
                    .example("POLYGON ((0.1 0.1, 0.4 0.1, 0.4 0.4, 0.1 0.4, 0.1 0.1))");

            // Registra o schema customizado globalmente
            openApi.getComponents().getSchemas().put("Polygon", polygonSchema);
        };
    }


    /**
     * Configura a URL do servidor da API no Swagger.
     * Forçando o Swagger a usar a URL base definida no env,
     * Config usada pra resolver problema de "Mixed Content" (HTTP/HTTPS)
     * ao acessar a documentação através de um proxy reverso (Ngrok).
     *
     * @param baseUrl A URL base da aplicação, injetada a partir da propriedade 'application.base-url'.
     * @return Um objeto OpenAPI configurado com a URL do servidor correta.
     */
    @Bean
    public OpenAPI customOpenAPI(@Value("${application.base-url}") String baseUrl) {
        return new OpenAPI()
            .servers(List.of(new Server().url(baseUrl).description("URL do Ambiente Atual")));
    }
}