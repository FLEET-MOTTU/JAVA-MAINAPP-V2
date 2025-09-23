// package br.com.mottu.fleet.config;

// import io.swagger.v3.oas.models.media.Schema;
// import io.swagger.v3.oas.models.media.StringSchema;
// import org.springdoc.core.customizers.OpenApiCustomizer;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// @Configuration
// public class SwaggerConfig {

//     /**
//      * Este Bean customiza a geração do OpenAPI.
//      * Ele mapeia o tipo complexo 'Polygon' para um tipo simples 'string' na documentação.
//      * Isso evita que o gerador do Swagger quebre ao tentar analisar a classe Polygon.
//      */
//     @Bean
//     public OpenApiCustomizer openApiCustomizer() {
//         // A biblioteca do Swagger pode ter dificuldade em converter o tipo Polygon.
//         // Este customizador substitui o schema complexo do Polygon por um schema de String simples.
//         return openApi -> {
//             Schema<?> polygonSchema = new StringSchema()
//                     .description("Representação de um polígono em formato WKT (Well-Known Text)")
//                     .example("POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))");
//             openApi.getComponents().getSchemas().put("Polygon", polygonSchema);
//         };
//     }
// }