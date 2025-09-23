package br.com.mottu.fleet.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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