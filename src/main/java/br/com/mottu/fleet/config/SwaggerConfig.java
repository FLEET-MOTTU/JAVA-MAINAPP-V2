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
     * Customizador para o OpenAPI que
     * ensina o Swagger a exibir o tipo 'Polygon' do JTS
     * como uma String no formato WKT, com descrição e exemplo
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            Schema<?> polygonSchema = new StringSchema()
                    .description("Representação de um polígono em formato WKT (Well-Known Text)")
                    .example("POLYGON ((0.1 0.1, 0.4 0.1, 0.4 0.4, 0.1 0.4, 0.1 0.1))");
            openApi.getComponents().getSchemas().put("Polygon", polygonSchema);
        };
    }

    /**
     * Força o Swagger a usar a URL base da .env,
     * resolve 'Mixed Content' ao usar proxy reverso com Ngrok
     *
     * @param baseUrl A URL base da aplicação, injetada a partir de 'application.base-url'.
     * @return Um objeto OpenAPI configurado.
     */
    @Bean
    public OpenAPI customOpenAPI(@Value("${application.base-url}") String baseUrl) {
        return new OpenAPI()
            .servers(List.of(new Server().url(baseUrl).description("URL do Ambiente Atual")));
    }
}