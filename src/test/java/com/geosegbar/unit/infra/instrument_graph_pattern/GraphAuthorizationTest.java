package com.geosegbar.unit.infra.instrument_graph_pattern;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.infra.instrument_graph_pattern.services.GraphAccessGuard;

/**
 * Trava a autorização do domínio de gráficos.
 *
 * As flags viewGraphs / editGraphsLocal / editGraphsDefault existiam no
 * cadastro desde sempre, eram gravadas pela tela de permissões e nunca lidas
 * por ninguém: qualquer usuário autenticado criava, renomeava e apagava padrão
 * de gráfico — inclusive de barragem de outro cliente, porque também não havia
 * recorte por barragem. Seis usuários em produção tinham exatamente esse
 * perfil (viewGraphs sim, edição não) e conseguiam editar; foi o que o
 * colaborador relatou.
 *
 * O teste lê os serviços como texto porque o alvo aqui é estrutural: cada porta
 * de entrada precisa passar pelo guard. É isso que tem que quebrar se alguém
 * acrescentar um método e esquecer a checagem.
 */
@Tag("unit")
@DisplayName("Unit Tests - Autorização de gráficos")
class GraphAuthorizationTest extends BaseUnitTest {

    private static final String BASE = "src/main/java/com/geosegbar/infra/";

    private static Map<String, String> fontes;

    @BeforeAll
    static void carregar() throws IOException {
        fontes = Map.of(
                "pattern", ler(BASE + "instrument_graph_pattern/services/InstrumentGraphPatternService.java"),
                "axes", ler(BASE + "instrument_graph_axes/services/InstrumentGraphAxesService.java"),
                "properties", ler(BASE + "instrument_graph_customization_properties/services/InstrumentGraphCustomizationPropertiesService.java"),
                "folder", ler(BASE + "instrument_graph_pattern_folder/services/InstrumentGraphPatternFolderService.java"),
                "autoPattern", ler(BASE + "instrument/services/AutoPatternCreationService.java"));
    }

    private static String ler(String caminho) throws IOException {
        return Files.readString(Path.of(caminho), StandardCharsets.UTF_8);
    }

    /**
     * Corpo do método a partir da assinatura, até a próxima assinatura pública.
     */
    private static String corpoDoMetodo(String fonte, String assinatura) {
        int inicio = fonte.indexOf(assinatura);
        assertThat(inicio).as("método %s existe", assinatura).isGreaterThan(0);
        int fim = fonte.indexOf("\n    public ", inicio + assinatura.length());
        return fim > 0 ? fonte.substring(inicio, fim) : fonte.substring(inicio);
    }

    @ParameterizedTest(name = "{0} · {1} exige permissão de edição")
    @CsvSource({
        "pattern,    'public GraphPatternResponseDTO create(',            checkCreatePatternForInstrument",
        "pattern,    'public GraphPatternDetailResponseDTO updateNameGraphPattern(', checkEditPattern",
        "pattern,    'public void deleteById(',                           checkEditPattern",
        "axes,       'public GraphAxesResponseDTO updateAxes(',           checkEditPattern",
        "properties, 'public void updateProperties(',                     checkEditPattern",
        "properties, 'public PropertyResponseDTO updateProperty(',        checkEditProperty",
        "properties, 'public UpdatePropertiesBatchResponseDTO updatePropertiesBatch(', checkEditPattern",
        "folder,     'public FolderResponseDTO create(',                  checkCreateFolderInDam",
        "folder,     'public FolderResponseDTO update(',                  checkEditFolder",
        "folder,     'public void delete(',                               checkEditFolder"
    })
    @DisplayName("Toda escrita em gráfico passa pelo guard")
    void escritasSaoGuardadas(String servico, String assinatura, String checagem) {
        assertThat(corpoDoMetodo(fontes.get(servico), assinatura))
                .withFailMessage("%s: %s não chama graphAccessGuard.%s", servico, assinatura, checagem)
                .contains("graphAccessGuard." + checagem + "(");
    }

    @ParameterizedTest(name = "{0} · {1} exige permissão de visualização")
    @CsvSource({
        "pattern,    'public List<GraphPatternResponseDTO> findByInstrument(',              checkViewByInstrument",
        "pattern,    'public List<GraphPatternDetailResponseDTO> findByInstrumentWithDetails(', checkViewByInstrument",
        "pattern,    'public List<GraphPatternDetailResponseDTO> findAllPatternsByDam(',    checkViewByDam",
        "pattern,    'public GraphPatternDetailResponseDTO findByIdWithDetails(',           checkViewByPattern",
        "pattern,    'public GraphPatternResponseDTO findSimpleById(',                      checkViewByPattern",
        "properties, 'public GraphPropertiesResponseDTO findByPatternId(',                  checkViewByPattern",
        "folder,     'public FolderWithPatternsDetailResponseDTO findByIdWithPatternsDetails(', checkViewByFolder",
        "folder,     'public DamFoldersWithPatternsDetailResponseDTO findFoldersWithPatternsDetailsByDam(', checkViewByDam"
    })
    @DisplayName("Toda leitura de gráfico passa pelo guard")
    void leiturasSaoGuardadas(String servico, String assinatura, String checagem) {
        assertThat(corpoDoMetodo(fontes.get(servico), assinatura))
                .withFailMessage("%s: %s não chama graphAccessGuard.%s", servico, assinatura, checagem)
                .contains("graphAccessGuard." + checagem + "(");
    }

    @Test
    @DisplayName("O cadastro de instrumento não é barrado pela permissão de gráfico")
    void cadastroDeInstrumentoNaoPassaPeloGuard() {
        // O "Padrão Automático" nasce dentro do cadastro do instrumento. Se
        // essa rotina usasse os métodos guardados, quem tem permissão de criar
        // instrumento mas não de editar gráfico deixaria de conseguir cadastrar
        // instrumento — trocar um furo de permissão por uma quebra de fluxo.
        String auto = fontes.get("autoPattern");

        assertThat(auto)
                .withFailMessage("AutoPatternCreationService precisa usar createInternal, "
                        + "senão o cadastro de instrumento passa a exigir permissão de gráfico.")
                .contains("graphPatternService.createInternal(")
                .doesNotContain("graphPatternService.create(");

        assertThat(auto)
                .withFailMessage("AutoPatternCreationService precisa usar updatePropertiesInternal.")
                .contains("propertiesService.updatePropertiesInternal(")
                .doesNotContain("propertiesService.updateProperties(patternId");
    }

    @Test
    @DisplayName("Os métodos internos não são expostos em controller")
    void metodosInternosNaoSaoExpostos() throws IOException {
        for (String controller : List.of(
                BASE + "instrument_graph_pattern/web/InstrumentGraphPatternController.java",
                BASE + "instrument_graph_customization_properties/web/InstrumentGraphCustomizationPropertiesController.java")) {

            assertThat(ler(controller))
                    .withFailMessage("%s expõe um método interno, que não checa permissão.", controller)
                    .doesNotContain("createInternal")
                    .doesNotContain("updatePropertiesInternal");
        }
    }

    @Test
    @DisplayName("O guard recarrega o usuário com as permissões antes de decidir")
    void guardRecarregaPermissoes() throws IOException {
        // O usuário do token vem do filtro e pode estar destacado, com
        // damPermissions e instrumentationPermission não inicializados. Decidir
        // sobre uma coleção não carregada é decidir sobre nada.
        String guard = ler(BASE + "instrument_graph_pattern/services/GraphAccessGuard.java");
        assertThat(guard).contains("findByIdWithPermissions");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Padrão Automático - PZ-01", "Padrão Automático - Régua 3"})
    @DisplayName("Padrão criado pelo sistema é reconhecido como padrão default")
    void reconhecePadraoAutomatico(String nome) {
        assertThat(GraphAccessGuard.isAutoPattern(nome)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Meu gráfico", "Comparativo 2026", "padrão automático - minusculo"})
    @DisplayName("Padrão criado por gente é local")
    void reconhecePadraoLocal(String nome) {
        assertThat(GraphAccessGuard.isAutoPattern(nome)).isFalse();
    }

    @Test
    @DisplayName("Nome nulo não estoura e não vira padrão default")
    void nomeNuloNaoEstoura() {
        assertThat(GraphAccessGuard.isAutoPattern(null)).isFalse();
    }
}
