package com.geosegbar.unit.configs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;

/**
 * Trava as defesas contra a queda por memória de 24/08/2026.
 *
 * O download do PSB compartilhado montava o ZIP inteiro em memória, e o
 * controller ainda duplicava com toByteArray(). Duas requisições esgotaram o
 * heap. Pior: a JVM não morreu — ficou 18 horas com o processo vivo, sem socket
 * em escuta e sem consumir CPU, porque não havia ExitOnOutOfMemoryError.
 *
 * São três defesas independentes, e este teste guarda todas:
 *   1. o ZIP é transmitido em fluxo, nunca acumulado
 *   2. a JVM morre no OutOfMemoryError, para o Docker reiniciá-la
 *   3. requisição assíncrona tem teto, para não segurar thread indefinidamente
 */
@Tag("unit")
@DisplayName("Unit Tests - Resiliência a memória e threads")
class ResilienciaMemoriaTest extends BaseUnitTest {

    private static String dockerfile;
    private static String properties;
    private static String zipService;
    private static String shareController;

    @BeforeAll
    static void carregar() throws IOException {
        dockerfile = ler("Dockerfile");
        properties = ler("src/main/resources/application.properties");
        zipService = ler("src/main/java/com/geosegbar/infra/share_folder/services/ZipService.java");
        shareController = ler("src/main/java/com/geosegbar/infra/share_folder/web/ShareFolderController.java");
    }

    private static String ler(String caminho) throws IOException {
        return Files.readString(Path.of(caminho), StandardCharsets.UTF_8);
    }

    /**
     * Ignora comentários: eles descrevem justamente o padrão que foi abandonado.
     */
    private static String semComentarios(String fonte, String marcador) {
        return fonte.lines()
                .filter(l -> !l.strip().startsWith(marcador))
                .filter(l -> !l.strip().startsWith("*"))
                .filter(l -> !l.strip().startsWith("/*"))
                .reduce("", (a, b) -> a + "\n" + b);
    }

    // ------------------------------------------------- 1) ZIP em fluxo
    @Test
    @DisplayName("O ZIP não é acumulado em memória")
    void zipIsStreamedNotBuffered() {
        String codigo = semComentarios(zipService, "//");

        assertThat(codigo)
                .withFailMessage("ZipService voltou a acumular o ZIP em memória. "
                        + "Uma pasta de PSB pode ter arquivos de 512 MB cada.")
                .doesNotContain("ByteArrayOutputStream");

        assertThat(codigo)
                .withFailMessage("ZipService deve escrever num OutputStream fornecido pelo chamador")
                .contains("writeZipToStream");
    }

    @Test
    @DisplayName("O controller transmite o ZIP em vez de materializá-lo")
    void controllerStreamsZip() {
        String codigo = semComentarios(shareController, "//");

        assertThat(codigo).contains("StreamingResponseBody");
        assertThat(codigo)
                .withFailMessage("toByteArray() no controller duplica o ZIP inteiro na memória")
                .doesNotContain("toByteArray");
    }

    @Test
    @DisplayName("Arquivos são copiados em fluxo, não carregados inteiros")
    void filesAreStreamedFromStorage() {
        String codigo = semComentarios(zipService, "//");

        assertThat(codigo)
                .withFailMessage("downloadFileBytes carrega o arquivo inteiro; use openStream")
                .doesNotContain("downloadFileBytes");
        assertThat(codigo).contains("openStream");
    }

    // ------------------------------- 2) JVM morre no OOM em vez de virar zumbi
    @Test
    @DisplayName("A JVM encerra ao esgotar o heap, para o Docker reiniciá-la")
    void jvmExitsOnOutOfMemory() {
        assertThat(dockerfile)
                .withFailMessage("Sem ExitOnOutOfMemoryError a JVM sobrevive ao OOM em estado "
                        + "inutilizável — foram 18 horas fora do ar em 24/08/2026.")
                .contains("-XX:+ExitOnOutOfMemoryError");

        assertThat(dockerfile).contains("-XX:+HeapDumpOnOutOfMemoryError");
    }

    @Test
    @DisplayName("As flags de segurança valem mesmo se o ambiente sobrescrever JAVA_OPTS")
    void safetyFlagsSurviveEnvironmentOverride() {
        // O deploy passa -e JAVA_OPTS a partir do .env.prod, sobrescrevendo o
        // padrão da imagem. Por isso as flags de segurança vão numa variável
        // separada, aplicada DEPOIS de JAVA_OPTS na linha de comando.
        assertThat(dockerfile).contains("JAVA_SAFETY_OPTS");
        assertThat(dockerfile)
                .withFailMessage("JAVA_SAFETY_OPTS precisa vir depois de JAVA_OPTS para prevalecer")
                .contains("java $JAVA_OPTS $JAVA_SAFETY_OPTS");
    }

    @Test
    @DisplayName("O heap padrão acompanha o limite do container, sem -Xmx fixo")
    void heapSizedFromContainerLimit() {
        // Xmx fixo igual ao limite do container faz o kernel matar o processo
        // antes de a JVM atingir o próprio teto: metaspace, pilhas de thread e
        // buffers diretos vivem fora do heap. Era o caso de homologação
        // (-Xmx2048m num container de 2g).
        String envJavaOpts = dockerfile.lines()
                .filter(l -> l.startsWith("ENV JAVA_OPTS="))
                .findFirst().orElse("");

        assertThat(envJavaOpts)
                .withFailMessage("O padrão da imagem não deve fixar -Xmx; use MaxRAMPercentage")
                .doesNotContain("-Xmx");
        assertThat(envJavaOpts).contains("MaxRAMPercentage");
    }

    // ---------------------------------------- 3) requisição assíncrona com teto
    @Test
    @DisplayName("Requisição assíncrona tem teto, para não reter thread para sempre")
    void asyncRequestsHaveTimeout() {
        String linha = properties.lines()
                .filter(l -> l.startsWith("spring.mvc.async.request-timeout="))
                .findFirst()
                .orElse("");

        assertThat(linha)
                .withFailMessage("Sem teto, uma leitura travada no S3 segura a thread do Tomcat "
                        + "indefinidamente; com max-threads=100, 100 downloads travados param a API.")
                .isNotEmpty();

        long timeout = Long.parseLong(linha.split("=")[1].trim());
        assertThat(timeout).isPositive();
    }

    @Test
    @DisplayName("O pool de threads do Tomcat continua limitado")
    void tomcatThreadPoolIsBounded() {
        assertThat(properties).contains("server.tomcat.max-threads=");
        assertThat(properties).contains("server.tomcat.max-connections=");
    }
}
