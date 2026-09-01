package com.geosegbar.unit.infra.file_storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * O download de arquivo único deixou de passar pelo backend.
 *
 * Antes, o Spring abria a própria conexão com o S3 e repassava os bytes: o
 * arquivo trafegava duas vezes (S3 -> Spring -> cliente), uma thread do Tomcat
 * ficava presa pela transferência inteira, e o proxy_read_timeout de 5 minutos
 * do nginx derrubava qualquer arquivo grande — 6,1 GB a 100 Mbps levam ~8min.
 *
 * A assinatura é cálculo puro, sem rede: dá para conferir a URL gerada num
 * teste unitário, com credenciais falsas.
 */
@Tag("unit")
@DisplayName("Unit Tests - Download pré-assinado")
class PresignedDownloadTest extends BaseUnitTest {

    private static final String BUCKET = "geosegbar-prod";

    /**
     * Mesma montagem do FileStorageService: filename ASCII de reserva mais
     * filename* com o nome real, no formato do RFC 6266.
     */
    private String contentDisposition(String filename) {
        String ascii = filename.replaceAll("[^\\x20-\\x7E]", "_").replace("\\", "_").replace("\"", "_");
        String utf8 = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + utf8;
    }

    private String presign(String key, String filename, String contentType) {
        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("AKIAEXEMPLO", "segredoDeTeste")))
                .build()) {

            GetObjectRequest.Builder req = GetObjectRequest.builder().bucket(BUCKET).key(key);

            if (filename != null) {
                req.responseContentDisposition(contentDisposition(filename));
            }
            if (contentType != null) {
                req.responseContentType(contentType);
            }

            return presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(30))
                    .getObjectRequest(req.build())
                    .build())
                    .url().toString();
        }
    }

    @Test
    @DisplayName("A URL é assinada e aponta direto para o S3")
    void urlApontaParaOS3EEhAssinada() {
        String url = presign("psb/1/relatorio.pdf", "relatorio.pdf", "application/pdf");

        assertThat(url).startsWith("https://" + BUCKET + ".s3.");
        assertThat(url)
                .contains("X-Amz-Signature=")
                .contains("X-Amz-Credential=")
                .contains("X-Amz-Expires=1800");
    }

    @Test
    @DisplayName("O navegador recebe o nome original, não a chave interna do S3")
    void nomeDoArquivoVemDoOriginal() {
        // O upload prefixa a chave com timestamp ("1756... _relatorio.pdf").
        // Sem sobrescrever o Content-Disposition na assinatura, era esse nome
        // que o usuário veria no arquivo baixado.
        String url = presign("psb/1/1756123456_relatorio_final.pdf",
                "Relatório Final 2026.pdf", "application/pdf");

        String decodificada = URLDecoder.decode(url, StandardCharsets.UTF_8);

        assertThat(decodificada)
                .withFailMessage("sem sobrescrever o Content-Disposition, o usuário "
                        + "baixaria o arquivo com a chave interna do S3 como nome")
                .contains("response-content-disposition=attachment")
                .doesNotContain("1756123456_relatorio_final.pdf\"");

        // O nome real vai no filename*, percent-encoded (RFC 5987) — continua
        // codificado mesmo depois de desfazer a codificação da própria URL.
        assertThat(decodificada)
                .contains("filename*=UTF-8''")
                .contains("Relat%C3%B3rio%20Final%202026.pdf");

        // E o filename simples é ASCII puro, sem palavra-codificada do RFC 2047.
        assertThat(decodificada)
                .contains("filename=\"Relat_rio Final 2026.pdf\"")
                .doesNotContain("=?UTF-8?Q?");
    }

    @Test
    @DisplayName("Nome com acento e espaço não quebra a URL")
    void nomeComAcentoNaoQuebra() {
        String url = presign("psb/1/arq.pdf", "Inspeção de Rotina — março.pdf", "application/pdf");

        assertThat(url)
                .doesNotContain(" ")
                .contains("X-Amz-Signature=");
    }

    @Test
    @DisplayName("O ZIP continua sendo transmitido em fluxo pelo backend")
    void zipContinuaStreamado() throws IOException {
        // O ZIP combina vários arquivos e por isso NÃO dá para redirecionar
        // para o S3. Ele tem que continuar passando pelo backend — mas em
        // fluxo, nunca acumulado. Foi acumular que derrubou a aplicação por 18
        // horas em 24/08/2026.
        String zipService = Files.readString(
                Path.of("src/main/java/com/geosegbar/infra/share_folder/services/ZipService.java"),
                StandardCharsets.UTF_8);

        // Procura USO, não menção: o javadoc da classe cita os dois nomes
        // justamente para explicar o que derrubou a aplicação.
        assertThat(zipService)
                .contains("openStream(")
                .doesNotContain("downloadFileBytes(")
                .doesNotContain("new ByteArrayOutputStream");

        String controller = Files.readString(
                Path.of("src/main/java/com/geosegbar/infra/share_folder/web/ShareFolderController.java"),
                StandardCharsets.UTF_8);

        assertThat(controller).contains("StreamingResponseBody");
    }

    @Test
    @DisplayName("Nenhuma rota de arquivo único repassa bytes pelo backend")
    void rotasDeArquivoUnicoNaoFazemProxy() throws IOException {
        for (String caminho : new String[]{
            "src/main/java/com/geosegbar/infra/psb/web/PSBController.java",
            "src/main/java/com/geosegbar/infra/share_folder/web/ShareFolderController.java"}) {

            String fonte = Files.readString(Path.of(caminho), StandardCharsets.UTF_8);

            assertThat(fonte)
                    .withFailMessage("%s voltou a devolver Resource: os bytes passariam pelo "
                            + "Spring de novo, e o timeout do nginx volta junto.", caminho)
                    .doesNotContain("ResponseEntity<Resource>")
                    .doesNotContain("UrlResource");
        }
    }

    @Test
    @DisplayName("O nginx não bufferiza o ZIP e tem timeout de transferência")
    void nginxPreparadoParaOZip() throws IOException {
        String conf = Files.readString(
                Path.of("nginx/default.conf.template"), StandardCharsets.UTF_8);

        // Com buffering ligado o nginx acumula a resposta em disco antes de
        // entregar, anulando o streaming do ZipService.
        assertThat(conf)
                .withFailMessage("A rota do ZIP precisa de proxy_buffering off")
                .contains("location ~ ^/share/download/")
                .contains("proxy_buffering off");

        assertThat(conf)
                .withFailMessage("300s não cobre transferência de arquivo grande")
                .doesNotContain("proxy_read_timeout 300s")
                .doesNotContain("proxy_send_timeout 300s");
    }
}
