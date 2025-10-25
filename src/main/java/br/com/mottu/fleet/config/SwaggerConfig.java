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
 * Configuração do Swagger (OpenAPI) para o sistema FLEET.
 *
 * Profile-aware:
 * - "dev": gera documentação usando HTTP (localhost, ambiente local)
 * - "prod": força HTTPS e define URL pública
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

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Value("${application.base-url:}")
    private String baseUrl;


    /**
     * Define o schema customizado do tipo Polygon (da lib JTS),
     * representando polígonos no formato WKT.
     */
    @Bean
    public OpenApiCustomizer polygonSchemaCustomizer() {
        return openApi -> {
            Schema<?> polygonSchema = new StringSchema()
                    .description("Representação de um polígono em formato WKT (Well-Known Text)")
                    .example("POLYGON ((0.1 0.1, 0.4 0.1, 0.4 0.4, 0.1 0.4, 0.1 0.1))");

            openApi.getComponents().getSchemas().put("Polygon", polygonSchema);
        };
    }


    /**
     * Configura o servidor base (HTTP ou HTTPS) exibido no Swagger.
     * O comportamento muda conforme o profile ativo.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        String resolvedUrl;

        if ("dev".equalsIgnoreCase(activeProfile)) {
            resolvedUrl = baseUrl.isBlank()
                    ? "http://localhost:8080"
                    : baseUrl.replace("https://", "http://");
        } else {
            if (baseUrl.isBlank()) {
                resolvedUrl = "https://fleet-app-journeytiago7.westus2.azurecontainer.io";
            } else {
                resolvedUrl = baseUrl.replace("http://", "https://");
            }
        }

        return new OpenAPI()
                .servers(List.of(new Server()
                        .url(resolvedUrl)
                        .description("Ambiente: " + activeProfile.toUpperCase())));
    }
}
