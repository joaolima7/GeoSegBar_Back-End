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
 * /actuator/env e /actuator/configprops, que expõem segredos.
 *
 * Desde 26/10/2026 o actuator escuta numa porta de management própria (8081),
 * que o nginx não encaminha, e expõe só health, prometheus e info. Os testes
 * abaixo travam as três camadas: liberação no Spring Security, exposição e porta.
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

    private static final Path APPLICATION_PROPERTIES = Path.of(
            "src/main/resources/application.properties");

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
    @DisplayName("O info do actuator é liberado só pelo caminho exato")
    void infoEndpointIsPublic() {
        assertThat(config).contains("\"/actuator/info\").permitAll()");
    }

    @Test
    @DisplayName("O actuator mora numa porta de management separada da API")
    void actuatorRunsOnSeparateManagementPort() throws IOException {
        String props = Files.readString(APPLICATION_PROPERTIES, StandardCharsets.UTF_8);

        assertThat(props).contains("management.server.port=${MANAGEMENT_PORT:8081}");
        // O nginx encaminha só a 9090: a porta de management nunca aparece nele.
        String nginx = Files.readString(Path.of("nginx/default.conf.template"), StandardCharsets.UTF_8);
        assertThat(nginx)
                .withFailMessage("O nginx não pode encaminhar para a porta de management (8081).")
                .doesNotContain("8081");
    }

    @Test
    @DisplayName("A exposição do actuator é a lista mínima, nunca curinga")
    void exposureIsMinimal() throws IOException {
        String props = Files.readString(APPLICATION_PROPERTIES, StandardCharsets.UTF_8);

        assertThat(props).contains("management.endpoints.web.exposure.include=health,prometheus,info");
        assertThat(props)
                .withFailMessage("exposure.include=* publicaria env, configprops e heapdump.")
                .doesNotContain("management.endpoints.web.exposure.include=*");
        assertThat(props).contains("management.endpoint.health.show-details=never");
    }

    @Test
    @DisplayName("HEALTHCHECK do Docker e gate do deploy sondam a porta de management")
    void probesTargetManagementPort() throws IOException {
        String dockerfile = Files.readString(Path.of("Dockerfile"), StandardCharsets.UTF_8);
        assertThat(dockerfile).contains("http://localhost:8081/actuator/health/liveness");

        String deploy = Files.readString(Path.of("bash/scripts/deploy_vps.sh"), StandardCharsets.UTF_8);
        assertThat(deploy).contains("http://localhost:8081/actuator/health/readiness");
        assertThat(deploy).doesNotContain("http://localhost:9090/actuator/health/readiness");
    }

    @Test
    @DisplayName("A liberação das sondas não é restrita a GET — HEAD precisa passar")
    void probesAreNotRestrictedToGet() {
        // `wget --spider`, usado pelo HEALTHCHECK do Docker e pelo gate do deploy,
        // envia HEAD. Com o matcher preso a HttpMethod.GET o HEAD cai em
        // anyRequest().authenticated() e volta 401 — a aplicação sobe saudável e
        // o deploy a rejeita assim mesmo. Aconteceu em homolog em 24/08/2026.
        assertThat(configSemComentarios)
                .withFailMessage("O matcher do actuator não pode ser restrito a um método HTTP: "
                        + "sondas usam HEAD e seriam bloqueadas.")
                .doesNotContain("HttpMethod.GET, \"/actuator");
    }

    @Test
    @DisplayName("As sondas do deploy e do Docker usam GET, não HEAD")
    void probeCommandsUseGet() throws IOException {
        for (String caminho : List.of(
                "bash/scripts/deploy_vps.sh",
                "bash/scripts/rollback_prod.sh",
                "Dockerfile")) {

            String conteudo = Files.readString(Path.of(caminho), StandardCharsets.UTF_8);
            if (!conteudo.contains("/actuator/health")) {
                continue;
            }

            // Sem as linhas de comentário: elas explicam por que --spider foi
            // abandonado, e citá-lo na explicação não pode reprovar o teste.
            String comandos = conteudo.lines()
                    .filter(linha -> !linha.strip().startsWith("#"))
                    .reduce("", (a, b) -> a + "\n" + b);

            assertThat(comandos)
                    .withFailMessage("%s usa --spider (HEAD) para sondar o actuator; "
                            + "use -O /dev/null (GET).", caminho)
                    .doesNotContain("--spider");
        }
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

        // Fora de conf.d de propósito: lá o nginx auto-inclui no contexto http,
        // onde `set` é inválido, e o servidor recusa a subir.
        assertThat(nginx).contains("include /etc/nginx/upstream_active.conf;");
        assertThat(nginx)
                .withFailMessage("O upstream não pode ficar em conf.d — o nginx auto-inclui "
                        + "no contexto http e a diretiva set é recusada lá.")
                .doesNotContain("/etc/nginx/conf.d/upstream_active.conf");
        assertThat(nginx).contains("proxy_pass http://$upstream_server;");

        // O arquivo apontado pelo include é gerado em runtime/ pelo deploy — fora
        // do git, para que reescrevê-lo a cada publicação não conflite no git pull.
        String deploy = Files.readString(
                Path.of("bash/scripts/deploy_vps.sh"), StandardCharsets.UTF_8);
        assertThat(deploy).contains("UPSTREAM_FILE=\"$RUNTIME_DIR/upstream_active.conf\"");
        assertThat(deploy).contains("nginx -s reload");
    }
}
