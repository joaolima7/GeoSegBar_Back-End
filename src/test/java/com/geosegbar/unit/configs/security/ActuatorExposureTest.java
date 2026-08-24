package com.geosegbar.unit.configs.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;

/**
 * Trava quais rotas do actuator são públicas.
 *
 * O container ficou "unhealthy" 29.238 vezes seguidas e o Prometheus nunca
 * coletou uma métrica porque tudo sob /actuator respondia 403 —
 * anyRequest().authenticated() alcançava também as sondas. Liberar health e
 * prometheus resolve; liberar /actuator/** resolveria também, e publicaria
 * /actuator/env e /actuator/configprops, que expõem segredos, já que
 * management.endpoints.web.exposure.include=*.
 *
 * O teste lê a configuração como texto de propósito: o alvo aqui é a decisão de
 * quais caminhos são liberados, e é isso que precisa quebrar se alguém ampliar a
 * liberação sem perceber a consequência.
 */
@Tag("unit")
@DisplayName("Unit Tests - Exposição pública do actuator")
class ActuatorExposureTest extends BaseUnitTest {

    private static final Path SECURITY_CONFIG = Path.of(
            "src/main/java/com/geosegbar/configs/security/SecurityConfig.java");

    private static String config;

    /**
     * Config sem as linhas de comentário. Os comentários citam nominalmente
     * /actuator/env e /actuator/configprops para explicar por que NÃO são
     * liberados — checar o arquivo cru acusaria justamente a explicação.
     */
    private static String configSemComentarios;

    @BeforeAll
    static void carregar() throws IOException {
        config = Files.readString(SECURITY_CONFIG, StandardCharsets.UTF_8);
        configSemComentarios = config.lines()
                .filter(linha -> !linha.strip().startsWith("//"))
                .filter(linha -> !linha.strip().startsWith("*"))
                .filter(linha -> !linha.strip().startsWith("/*"))
                .reduce("", (a, b) -> a + "\n" + b);
    }

    @Test
    @DisplayName("As sondas de saúde são públicas — sem elas o healthcheck e o deploy não funcionam")
    void healthProbesArePublic() {
        assertThat(config).contains("\"/actuator/health\", \"/actuator/health/**\").permitAll()");
    }

    @Test
    @DisplayName("O endpoint de métricas é público para o Prometheus coletar")
    void prometheusEndpointIsPublic() {
        assertThat(config).contains("\"/actuator/prometheus\").permitAll()");
    }

    @Test
    @DisplayName("Nenhum curinga amplo sobre /actuator — isso publicaria env e configprops")
    void noBroadActuatorWildcard() {
        List<String> proibidos = List.of(
                "\"/actuator/**\").permitAll()",
                "\"/actuator/*\").permitAll()",
                "\"/actuator\").permitAll()");

        assertThat(proibidos)
                .allSatisfy(padrao -> assertThat(configSemComentarios)
                .withFailMessage(
                        "SecurityConfig libera %s. Com exposure.include=*, isso publica "
                        + "/actuator/env e /actuator/configprops, que carregam segredos. "
                        + "Libere apenas health e prometheus.", padrao)
                .doesNotContain(padrao));
    }

    @Test
    @DisplayName("Endpoints sensíveis do actuator continuam sem liberação explícita")
    void sensitiveEndpointsRemainProtected() {
        assertThat(configSemComentarios).doesNotContain("/actuator/env");
        assertThat(configSemComentarios).doesNotContain("/actuator/configprops");
        assertThat(configSemComentarios).doesNotContain("/actuator/heapdump");
        assertThat(configSemComentarios).doesNotContain("/actuator/threaddump");
    }

    @Test
    @DisplayName("O nginx bloqueia /actuator/ vindo da internet")
    void nginxBlocksActuatorPublicly() throws IOException {
        String nginx = Files.readString(
                Path.of("nginx/default.conf.template"), StandardCharsets.UTF_8);

        assertThat(nginx).contains("location /actuator/");
        assertThat(nginx).contains("deny all;");
    }

    @Test
    @DisplayName("O nginx resolve o upstream por arquivo — é o que permite trocar versão sem downtime")
    void nginxUsesSwitchableUpstream() throws IOException {
        String nginx = Files.readString(
                Path.of("nginx/default.conf.template"), StandardCharsets.UTF_8);

        assertThat(nginx).contains("include /etc/nginx/conf.d/upstream_active.conf;");
        assertThat(nginx).contains("proxy_pass http://$upstream_server;");

        // O arquivo apontado pelo include é gerado em runtime/ pelo deploy — fora
        // do git, para que reescrevê-lo a cada publicação não conflite no git pull.
        String deploy = Files.readString(
                Path.of("bash/scripts/deploy_vps.sh"), StandardCharsets.UTF_8);
        assertThat(deploy).contains("UPSTREAM_FILE=\"$RUNTIME_DIR/upstream_active.conf\"");
        assertThat(deploy).contains("nginx -s reload");
    }
}
