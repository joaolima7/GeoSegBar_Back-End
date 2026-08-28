package com.geosegbar.unit.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.geosegbar.common.utils.ChecklistOptionTransitionValidator;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.exceptions.InvalidInputException;

/**
 * Trava as regras de preenchimento de checklist descritas na especificação
 * "Regras de Preenchimento de Checklist — Mobile → Web".
 *
 * O documento é o contrato entre três implementações — app mobile, web e este
 * servidor. Já divergiram antes: o PV nunca exigiu observação no backend porque
 * a regra estava escrita em dois lugares e só um foi atualizado. As tabelas
 * abaixo são as do documento, célula por célula, para que uma divergência nova
 * quebre aqui em vez de aparecer no campo.
 */
@Tag("unit")
@DisplayName("Unit Tests - Regras de preenchimento de checklist")
class RegrasPreenchimentoChecklistTest extends BaseUnitTest {

    private static void transicao(String anterior, String nova) {
        ChecklistOptionTransitionValidator.validateTransition(anterior, nova, "Pergunta X");
    }

    // ===================================================== seção 2: transições
    @ParameterizedTest(name = "sem resposta anterior: {0} é permitida")
    @ValueSource(strings = {"PV", "NI", "NE"})
    @DisplayName("Primeira inspeção do ponto: só PV, NI e NE")
    void primeiraInspecaoPermiteApenasPvNiNe(String opcao) {
        assertThatCode(() -> transicao(null, opcao)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "sem resposta anterior: {0} é bloqueada")
    @ValueSource(strings = {"AU", "PC", "DM", "DS"})
    @DisplayName("Primeira inspeção não aceita evolução de anomalia que não existe")
    void primeiraInspecaoBloqueiaEvolucao(String opcao) {
        assertThatThrownBy(() -> transicao(null, opcao))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("não há resposta anterior");
    }

    @ParameterizedTest(name = "anterior={0}, nova={1} -> permitida")
    @CsvSource({
        // Depois de NE o ponto está limpo: volta ao leque inicial.
        "NE, PV", "NE, NI", "NE, NE",
        // Depois de PV existe anomalia: só cabe acompanhar a evolução.
        "PV, AU", "PV, PC", "PV, DM", "PV, DS", "PV, NI",
        // Durante a evolução, segue acompanhando.
        "AU, PC", "AU, DM", "AU, DS", "AU, AU", "AU, NI",
        "PC, AU", "PC, DM", "PC, DS", "PC, NI",
        "DM, AU", "DM, PC", "DM, DS", "DM, NI",
        // Depois de DS a anomalia sumiu: recomeça.
        "DS, PV", "DS, NI", "DS, NE"
    })
    @DisplayName("Tabela de disponibilidade — combinações permitidas")
    void combinacoesPermitidas(String anterior, String nova) {
        assertThatCode(() -> transicao(anterior, nova)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "anterior={0}, nova={1} -> bloqueada")
    @CsvSource({
        "NE, PC", "NE, AU", "NE, DM", "NE, DS",
        "PV, PV", "PV, NE",
        "AU, NE", "AU, PV",
        "PC, NE", "PC, PV",
        "DM, NE", "DM, PV",
        "DS, PC", "DS, AU", "DS, DM", "DS, DS"
    })
    @DisplayName("Tabela de disponibilidade — combinações bloqueadas")
    void combinacoesBloqueadas(String anterior, String nova) {
        assertThatThrownBy(() -> transicao(anterior, nova))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("inspeção anterior foi '" + anterior + "'");
    }

    @ParameterizedTest(name = "anterior={0}: NI continua permitido")
    @ValueSource(strings = {"NE", "PV", "AU", "PC", "DM", "DS"})
    @DisplayName("NI nunca é bloqueado, em nenhum cenário")
    void niNuncaEBloqueado(String anterior) {
        assertThatCode(() -> transicao(anterior, "NI")).doesNotThrowAnyException();
    }

    // ============================================ seção 3: campos obrigatórios
    private static void campos(String opcao, String observacao, boolean foto, boolean local) {
        campos(opcao, observacao, foto, local, false);
    }

    private static void campos(String opcao, String observacao, boolean foto, boolean local,
            boolean exigeObservacaoNoNi) {
        ChecklistOptionTransitionValidator.validateAnswerFields(
                opcao, observacao, foto, local, "Recomendação", 5L, 5L,
                exigeObservacaoNoNi, "Pergunta X");
    }

    @Test
    @DisplayName("NE não exige nada — é a única opção sem checagem")
    void neNaoExigeNada() {
        assertThatCode(() -> campos("NE", null, false, false)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("NI exige observação quando a regra do app está ligada")
    void niExigeObservacaoQuandoLigado() {
        assertThatThrownBy(() -> campos("NI", null, false, false, true))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Observação");

        assertThatCode(() -> campos("NI", "Acesso interditado", false, false, true))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("NI sem observação passa enquanto a regra está desligada — é o padrão de hoje")
    void niSemObservacaoPassaPorPadrao() {
        // 174 das 236 respostas NI já gravadas em produção não têm observação.
        // Ligar a regra sem alinhar as pontas passaria a recusar a maior parte
        // do que chega hoje.
        assertThatCode(() -> campos("NI", null, false, false)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("NI nunca exige foto nem localização, com a regra ligada ou não")
    void niNuncaExigeFotoNemLocalizacao() {
        assertThatCode(() -> campos("NI", "Acesso interditado", false, false, true))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} exige observação e foto")
    @ValueSource(strings = {"AU", "DM", "PC", "DS"})
    @DisplayName("Evolução de anomalia exige observação e foto — e nada além disso")
    void evolucaoExigeObservacaoEFoto(String opcao) {
        assertThatThrownBy(() -> campos(opcao, null, false, false))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Observação")
                .hasMessageContaining("Foto");

        // Sem localização: evolução não exige GPS.
        assertThatCode(() -> campos(opcao, "Fissura maior", true, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PV exige observação, foto e localização")
    void pvExigeTriade() {
        assertThatThrownBy(() -> campos("PV", null, false, false))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Observação")
                .hasMessageContaining("Foto")
                .hasMessageContaining("Localização");
    }

    @Test
    @DisplayName("PV sem observação é recusado — foto e localização não bastam")
    void pvSemObservacaoERecusado() {
        // O app sempre mandou observação no PV, então o servidor nunca exigiu e
        // ninguém notou. Um cliente que não mande grava uma anomalia muda.
        assertThatThrownBy(() -> campos("PV", "   ", true, true))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Observação");
    }

    @Test
    @DisplayName("PV exige também recomendação, nível de perigo e status")
    void pvExigeClassificacaoDaAnomalia() {
        assertThatThrownBy(() -> ChecklistOptionTransitionValidator.validateAnswerFields(
                "PV", "Trinca nova", true, true, null, null, null, false, "Pergunta X"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Recomendação")
                .hasMessageContaining("Nível de Perigo")
                .hasMessageContaining("Status");
    }

    @Test
    @DisplayName("PV completo passa")
    void pvCompletoPassa() {
        assertThatCode(() -> campos("PV", "Trinca nova no talude", true, true))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Na edição de resposta gravada, a classificação da anomalia não é cobrada")
    void edicaoNaoCobraClassificacaoDaAnomalia() {
        // O payload de correção carrega opção, comentário e fotos — os três
        // campos da anomalia não trafegam nele, então não há o que validar.
        assertThatCode(() -> ChecklistOptionTransitionValidator.validateEditedAnswerFields(
                "PV", "Trinca nova", true, true, false, "Pergunta X"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> ChecklistOptionTransitionValidator.validateEditedAnswerFields(
                "PV", "Trinca nova", false, true, false, "Pergunta X"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Foto");
    }

    // ================================= a regra anterior vem de uma fonte só
    @Test
    @DisplayName("A resposta anterior que o servidor valida é a mesma que a API publica")
    void respostaAnteriorTemFonteUnica() throws IOException {
        // A UI monta as opções disponíveis a partir do lastSelectedOption que a
        // API devolve. Se a consulta que valida o envio usar outro critério, a
        // tela oferece uma opção que o servidor recusa — e o inspetor só
        // descobre no fim, depois de preencher o checklist inteiro.
        String repo = Files.readString(Path.of(
                "src/main/java/com/geosegbar/infra/answer/persistence/jpa/AnswerRepository.java"),
                StandardCharsets.UTF_8);

        for (String consulta : List.of("findLastRelevantOptionLabels", "findRelevantOptionLabelsBefore")) {
            int inicio = repo.indexOf("List<Object[]> " + consulta);
            assertThat(inicio).as("consulta %s existe", consulta).isGreaterThan(0);

            String corpo = repo.substring(0, inicio);
            corpo = corpo.substring(corpo.lastIndexOf("@Query"));

            assertThat(corpo)
                    .withFailMessage("%s precisa ignorar NI, como findLatestNonNIAnswer faz — "
                            + "'não inspecionado' não é observação de campo.", consulta)
                    .contains("UPPER(o.label) <> 'NI'");

            assertThat(corpo)
                    .withFailMessage("%s não pode filtrar por checklist: o histórico do ponto é "
                            + "da pergunta dentro do questionário, e a consulta de leitura que "
                            + "alimenta a UI não filtra por checklist.", consulta)
                    .doesNotContain("checklist_id");

            // Mesmo escopo e mesma ordenação de findLatestNonNIAnswer, que é o
            // que a UI enxerga. Trocar por cr.created_at parece equivalente e
            // não é: são duas colunas distintas.
            assertThat(corpo)
                    .withFailMessage("%s precisa recortar por qr.dam_id e ordenar por "
                            + "qr.created_at, igual à consulta de leitura.", consulta)
                    .contains("qr.dam_id = :damId")
                    .contains("qr.created_at DESC");
        }
    }

    @Test
    @DisplayName("Ao corrigir uma resposta, a anterior é a de antes dela — não a última de todas")
    void edicaoOlhaParaTrasDaPropriaResposta() throws IOException {
        // Editar a inspeção de julho quando já existe uma de agosto tem que
        // julgar a transição contra o que havia em julho. Usar a última
        // resposta julgaria a correção contra o futuro dela.
        String repo = Files.readString(Path.of(
                "src/main/java/com/geosegbar/infra/answer/persistence/jpa/AnswerRepository.java"),
                StandardCharsets.UTF_8);

        int inicio = repo.indexOf("List<Object[]> findRelevantOptionLabelsBefore");
        String corpo = repo.substring(0, inicio);
        corpo = corpo.substring(corpo.lastIndexOf("@Query"));

        assertThat(corpo)
                .withFailMessage("a consulta de edição precisa da fronteira temporal")
                .contains("cr.created_at < :beforeDate");

        assertThat(corpo)
                .withFailMessage("a consulta de edição precisa excluir a própria resposta editada "
                        + "pelo ID; depender só da data é uma garantia acidental da ordem em que "
                        + "os dois created_at são gravados.")
                .contains("cr.id <> :checklistResponseId");
    }
}
