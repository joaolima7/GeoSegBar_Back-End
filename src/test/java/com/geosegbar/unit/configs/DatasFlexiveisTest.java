package com.geosegbar.unit.configs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.configs.web_config.LenientLocalDateTimeDeserializer;
import com.geosegbar.infra.share_folder.dtos.CreateShareFolderRequest;

/**
 * Datas de entrada aceitas em vários formatos.
 *
 * Em 25/08/2026 o compartilhamento de PSB quebrou porque o front mandou
 * "2026-08-26" — só a data, vinda de um date picker — num campo LocalDateTime.
 * O Jackson padrão só aceita ISO completo, e a requisição inteira falhava com
 * DateTimeParseException e mensagem incompreensível para quem estava usando.
 */
@Tag("unit")
@DisplayName("Unit Tests - Datas de entrada em formatos variados")
class DatasFlexiveisTest extends BaseUnitTest {

    private static ObjectMapper mapper;

    @BeforeAll
    static void configurar() {
        // Mesmo registro que o JacksonConfig faz na aplicação.
        SimpleModule modulo = new SimpleModule();
        modulo.addDeserializer(LocalDateTime.class, new LenientLocalDateTimeDeserializer());
        modulo.addDeserializer(java.time.LocalDate.class,
                new com.geosegbar.configs.web_config.LenientLocalDateDeserializer());
        mapper = new ObjectMapper().registerModule(modulo);
    }

    private LocalDateTime ler(String json) throws Exception {
        return mapper.readValue("{\"valor\":" + json + "}", Envelope.class).valor;
    }

    static class Envelope {

        public LocalDateTime valor;
    }

    // ------------------------------------------------------------ aceitos
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        // o formato exato que derrubou o compartilhamento
        "'\"2026-08-26\"',                      '2026-08-26T00:00'",
        "'\"2026-08-26T14:30\"',                '2026-08-26T14:30'",
        "'\"2026-08-26T14:30:00\"',             '2026-08-26T14:30'",
        "'\"2026-08-26T14:30:45.123\"',         '2026-08-26T14:30:45.123'",
        "'\"2026-08-26 14:30:00\"',             '2026-08-26T14:30'",
        "'\"2026-08-26 14:30\"',                '2026-08-26T14:30'",
        "'\"26/08/2026\"',                      '2026-08-26T00:00'",
        "'\"26/08/2026 14:30:00\"',             '2026-08-26T14:30'"
    })
    @DisplayName("Formatos comuns de front são aceitos")
    void aceitaFormatosComuns(String json, String esperado) throws Exception {
        assertThat(ler(json)).isEqualTo(LocalDateTime.parse(esperado));
    }

    @Test
    @DisplayName("Instante em UTC é convertido para o fuso da aplicação")
    void converteInstanteUtc() throws Exception {
        // 17:30Z = 14:30 em America/Sao_Paulo (UTC-3). Guardar 17:30 "cru"
        // adiantaria o horário em três horas.
        assertThat(ler("\"2026-08-26T17:30:00Z\""))
                .isEqualTo(LocalDateTime.parse("2026-08-26T14:30"));
    }

    @Test
    @DisplayName("Instante com deslocamento explícito é convertido")
    void converteInstanteComDeslocamento() throws Exception {
        assertThat(ler("\"2026-08-26T14:30:00-03:00\""))
                .isEqualTo(LocalDateTime.parse("2026-08-26T14:30"));
    }

    @Test
    @DisplayName("Epoch em milissegundos é aceito")
    void aceitaEpochMillis() throws Exception {
        long epoch = LocalDateTime.parse("2026-08-26T14:30")
                .atZone(java.time.ZoneId.of("America/Sao_Paulo"))
                .toInstant().toEpochMilli();

        assertThat(ler(String.valueOf(epoch)))
                .isEqualTo(LocalDateTime.parse("2026-08-26T14:30"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"\"", "\"   \""})
    @DisplayName("String vazia é ausência de valor, não erro")
    void stringVaziaViraNulo(String json) throws Exception {
        assertThat(ler(json)).isNull();
    }

    @Test
    @DisplayName("Nulo continua nulo")
    void nuloContinuaNulo() throws Exception {
        assertThat(ler("null")).isNull();
    }

    // ------------------------------------------------------------ recusados
    @Test
    @DisplayName("Formato irreconhecível dá erro claro, com os formatos aceitos")
    void formatoInvalidoTemMensagemUtil() {
        assertThatThrownBy(() -> ler("\"26 de agosto\""))
                .hasMessageContaining("formato não reconhecido")
                .hasMessageContaining("DD/MM/AAAA");
    }

    // ----------------------------------------------- campos de data pura
    @ParameterizedTest(name = "LocalDate: {0} -> {1}")
    @CsvSource({
        "'\"2026-08-26\"',                    '2026-08-26'",
        "'\"26/08/2026\"',                    '2026-08-26'",
        // Componente de calendário que manda datetime num campo de data — o
        // problema espelhado do que quebrou o compartilhamento.
        "'\"2026-08-26T14:30:00\"',           '2026-08-26'",
        "'\"2026-08-26 14:30:00\"',           '2026-08-26'"
    })
    @DisplayName("Campos LocalDate aceitam datetime e formato brasileiro")
    void localDateAceitaVariosFormatos(String json, String esperado) throws Exception {
        java.time.LocalDate lido = mapper
                .readValue("{\"valor\":" + json + "}", EnvelopeData.class).valor;
        assertThat(lido).isEqualTo(java.time.LocalDate.parse(esperado));
    }

    @Test
    @DisplayName("String vazia em LocalDate vira null")
    void localDateVazioViraNulo() throws Exception {
        assertThat(mapper.readValue("{\"valor\":\"\"}", EnvelopeData.class).valor).isNull();
    }

    static class EnvelopeData {

        public java.time.LocalDate valor;
    }

    // ------------------------------------------- semântica de expiração
    @Test
    @DisplayName("Data pura em expiresAt vale até o FIM do dia")
    void expiracaoPorDataPuraVaiAteOFimDoDia() throws Exception {
        String json = """
            {"psbFolderId":1,"sharedById":2,"sharedWithEmail":"a@b.com",
             "expiresAt":"2026-08-26"}
            """;

        CreateShareFolderRequest req = mapper.readValue(json, CreateShareFolderRequest.class);

        // Se fosse 00:00, o link nasceria vencido: qualquer acesso no dia 26 já
        // estaria expirado, e o usuário veria "este link expirou" no mesmo dia
        // em que o criou.
        assertThat(req.getExpiresAt()).isEqualTo(LocalDateTime.parse("2026-08-26T23:59:59"));
    }

    @Test
    @DisplayName("Hora explícita em expiresAt é respeitada como veio")
    void expiracaoComHoraExplicitaEPreservada() throws Exception {
        String json = """
            {"psbFolderId":1,"sharedById":2,"sharedWithEmail":"a@b.com",
             "expiresAt":"2026-08-26T10:00:00"}
            """;

        CreateShareFolderRequest req = mapper.readValue(json, CreateShareFolderRequest.class);

        assertThat(req.getExpiresAt()).isEqualTo(LocalDateTime.parse("2026-08-26T10:00"));
    }

    @Test
    @DisplayName("expiresAt ausente significa link sem validade")
    void expiracaoAusenteSignificaSemValidade() throws Exception {
        String json = """
            {"psbFolderId":1,"sharedById":2,"sharedWithEmail":"a@b.com"}
            """;

        CreateShareFolderRequest req = mapper.readValue(json, CreateShareFolderRequest.class);

        assertThat(req.getExpiresAt()).isNull();
    }
}
