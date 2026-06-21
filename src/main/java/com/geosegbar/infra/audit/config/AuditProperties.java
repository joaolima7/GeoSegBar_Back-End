package com.geosegbar.infra.audit.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Configurações do sistema de auditoria. Sobrescritas via {@code audit.*} nos
 * arquivos de propriedades.
 */
@Component
@ConfigurationProperties(prefix = "audit")
@Getter
@Setter
public class AuditProperties {

    /**
     * Liga/desliga a auditoria por completo.
     */
    private boolean enabled = true;

    /**
     * Se true, audita também requisições GET (leituras). Desligado por padrão
     * para evitar volume excessivo.
     */
    private boolean includeGet = false;

    /**
     * Tamanho máximo (em caracteres) de request/response body gravados. Acima
     * disso o conteúdo é truncado.
     */
    private int maxBodyLength = 16384;

    /**
     * Tamanho máximo (em caracteres) do stack trace gravado.
     */
    private int maxStackTraceLength = 8000;

    /**
     * Rotas ignoradas pela auditoria (prefixos). OPTIONS e estáticos não geram
     * registro.
     */
    private List<String> excludedPaths = Arrays.asList(
            "/actuator",
            "/uploads",
            "/psb/files/download",
            "/share/access",
            "/share/download"
    );

    /**
     * Nomes de campos (JSON) e headers a mascarar nos bodies/headers gravados.
     */
    private List<String> maskedFields = Arrays.asList(
            "password",
            "senha",
            "token",
            "authorization",
            "cookie",
            "set-cookie"
    );
}
