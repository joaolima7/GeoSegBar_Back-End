package com.geosegbar.configs.web_config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;

@Configuration
public class JacksonConfig {

    /**
     * Limite máximo, em caracteres, para UM único valor String no JSON.
     * <p>
     * O Jackson 2.15+ impõe um teto padrão de 20.000.000 (~20MB) por valor
     * String ({@code StreamReadConstraints.getMaxStringLength()}). Como as fotos
     * do checklist trafegam como base64 dentro do corpo JSON — e o base64 infla
     * o binário em ~33% — uma imagem de ~50MB vira ~68 milhões de caracteres e
     * estoura o teto. Elevamos para 100M (~73MB de imagem) com folga.
     */
    private static final int MAX_JSON_STRING_LENGTH = 100_000_000;

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // Aumenta o limite de tamanho de valores String (fotos base64 grandes).
        objectMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(MAX_JSON_STRING_LENGTH)
                        .build());

        Hibernate5JakartaModule hibernateModule = new Hibernate5JakartaModule();
        hibernateModule.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, false);
        objectMapper.registerModule(hibernateModule);

        return objectMapper;
    }
}