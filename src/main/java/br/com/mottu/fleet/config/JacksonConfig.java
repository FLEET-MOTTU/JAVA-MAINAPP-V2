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
     * tipos de data/hora (Instant, LocalDate, etc.)
     * 2. O Spring Boot usa esse ObjectMapper pra todas as operações de JSON
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