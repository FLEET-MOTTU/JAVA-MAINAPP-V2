package br.com.mottu.fleet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuração customizada para o Jackson (conversor de JSON).
 */
@Configuration
public class JacksonConfig {

    /**
     * Cria e expõe um bean ObjectMapper customizado.
     * 1. Registra o JavaTimeModule para que o Jackson saiba como serializar/desserializar
     * tipos de data/hora do Java 8+ (como Instant, LocalDate, etc.). Resolve o erro de
     * 'InvalidDefinitionException' para java.time.Instant.
     * 2. O Spring Boot reutilizará este ObjectMapper para todas as operações de JSON,
     * incluindo a conversão de partes de requisições multipart, resolvendo o erro de
     * 'Content-Type 'application/octet-stream' is not supported'.
     *
     * @return Um ObjectMapper configurado.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}