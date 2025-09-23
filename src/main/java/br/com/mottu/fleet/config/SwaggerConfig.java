package br.com.mottu.fleet.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class SwaggerConfig {

    /**
     * Este Bean customiza a geração do OpenAPI para o tipo Polygon.
     */
    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        return openApi -> {
            Schema<?> polygonSchema = new StringSchema()
                    .description("Representação de um polígono em formato WKT (Well-Known Text)")
                    .example("POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))");
            openApi.getComponents().getSchemas().put("Polygon", polygonSchema);
        };
    }
}