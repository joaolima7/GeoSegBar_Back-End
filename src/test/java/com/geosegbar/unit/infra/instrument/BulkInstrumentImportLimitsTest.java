package com.geosegbar.unit.infra.instrument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.instrument.dtos.CreateInstrumentRequest;
import com.geosegbar.infra.instrument.dtos.DeterministicLimitDTO;
import com.geosegbar.infra.instrument.dtos.ImportInstrumentsRequest;
import com.geosegbar.infra.instrument.services.BulkInstrumentImportService;
import com.geosegbar.infra.instrument.services.BulkInstrumentImportService.ImportResult;
import com.geosegbar.infra.instrument.services.InstrumentService;
import com.geosegbar.infra.instrument_type.persistence.jpa.InstrumentTypeRepository;
import com.geosegbar.infra.measurement_unit.persistence.jpa.MeasurementUnitRepository;
import com.geosegbar.infra.section.persistence.jpa.SectionRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("BulkInstrumentImportService - limites determinísticos")
class BulkInstrumentImportLimitsTest {

    @Mock
    private InstrumentService instrumentService;

    @Mock
    private DamService damService;

    @Mock
    private MeasurementUnitRepository muRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private InstrumentTypeRepository instrumentTypeRepository;

    @InjectMocks
    private BulkInstrumentImportService service;

    private ImportInstrumentsRequest meta;

    @BeforeEach
    void setUp() {
        MeasurementUnitEntity unit = new MeasurementUnitEntity();
        unit.setId(1L);
        unit.setAcronym("M");
        when(muRepository.findAll()).thenReturn(List.of(unit));

        when(damService.findById(anyLong())).thenReturn(new DamEntity());

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setId(1L);
        type.setName("PIEZOMETRO");
        when(instrumentTypeRepository.findByClientIsNullOrderByNameAsc()).thenReturn(List.of(type));

        when(sectionRepository.findAllByDamId(anyLong())).thenReturn(List.of());

        meta = new ImportInstrumentsRequest();
        meta.setDamId(1L);
        meta.setNoLimit(false);
        meta.setActiveForSection(false);
    }

    @Test
    @DisplayName("nível em branco vira nulo e o instrumento é importado")
    void blankLevelBecomesNull() throws IOException {
        byte[] file = workbook(new String[][]{
            {"PZ-02", "PZ", "Deterministico", "", "", "", "", "15"}
        });

        ImportResult result = service.importFromExcel(meta, xlsx(file));

        assertThat(result.getSuccess()).isEqualTo(1);
        assertThat(result.getFailures()).isZero();

        ArgumentCaptor<CreateInstrumentRequest> captor = ArgumentCaptor.forClass(CreateInstrumentRequest.class);
        verify(instrumentService).createComplete(captor.capture());
        DeterministicLimitDTO limit = captor.getValue().getOutputs().get(0).getDeterministicLimit();
        assertThat(limit.getAttentionValue()).isNull();
        assertThat(limit.getAlertValue()).isNull();
        assertThat(limit.getEmergencyValue()).isEqualByComparingTo(new BigDecimal("15"));
    }

    @Test
    @DisplayName("valor que não é número falha só aquele instrumento, com linha e coluna")
    void invalidNumberFailsOnlyThatInstrument() throws IOException {
        byte[] file = workbook(new String[][]{
            {"PZ-01", "PZ", "Deterministico", "", "", "10", "abc", "12"},
            {"PZ-02", "PZ", "Deterministico", "", "", "", "", "15"}
        });

        ImportResult result = service.importFromExcel(meta, xlsx(file));

        assertThat(result.getSuccess()).isEqualTo(1);
        assertThat(result.getFailures()).isEqualTo(1);
        assertThat(result.getErrors()).singleElement().satisfies(error -> assertThat(error)
                .contains("PZ-01")
                .contains("linha 2")
                .contains("'Valor de Alerta'")
                .contains("'abc'"));
        verify(instrumentService, times(1)).createComplete(any());
    }

    @Test
    @DisplayName("fórmula com resultado vazio vira nível não aplicado")
    void blankFormulaResultBecomesNull() throws IOException {
        byte[] file = workbook(new String[][]{
            {"PZ-03", "PZ", "Deterministico", "", "", "=IF(1=1,\"\",\"x\")", "10", "=6*2"}
        });

        ImportResult result = service.importFromExcel(meta, xlsx(file));

        assertThat(result.getFailures()).isZero();
        ArgumentCaptor<CreateInstrumentRequest> captor = ArgumentCaptor.forClass(CreateInstrumentRequest.class);
        verify(instrumentService).createComplete(captor.capture());
        DeterministicLimitDTO limit = captor.getValue().getOutputs().get(0).getDeterministicLimit();
        assertThat(limit.getAttentionValue()).isNull();
        assertThat(limit.getAlertValue()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(limit.getEmergencyValue()).isEqualByComparingTo(new BigDecimal("12"));
    }

    @Test
    @DisplayName("instrumento sem limites ignora a aba Limits, mesmo com valor inválido")
    void noLimitInstrumentIgnoresInvalidLimitRow() throws IOException {
        meta.setNoLimit(true);
        byte[] file = workbook(new String[][]{
            {"PZ-04", "PZ", "Deterministico", "", "", "N/A", "", ""}
        });

        ImportResult result = service.importFromExcel(meta, xlsx(file));

        assertThat(result.getSuccess()).isEqualTo(1);
        assertThat(result.getFailures()).isZero();
        ArgumentCaptor<CreateInstrumentRequest> captor = ArgumentCaptor.forClass(CreateInstrumentRequest.class);
        verify(instrumentService).createComplete(captor.capture());
        assertThat(captor.getValue().getOutputs().get(0).getDeterministicLimit()).isNull();
    }

    private static MockMultipartFile xlsx(byte[] content) {
        return new MockMultipartFile("file", "instrumentos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }

    private static byte[] workbook(String[][] limitRows) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet instruments = wb.createSheet("Instruments");
            row(instruments, 0, "ID", "Nome", "Tipo de Instrumento", "Localização");
            Sheet outputs = wb.createSheet("Outputs");
            row(outputs, 0, "ID", "Sigla", "Nome", "Equação", "Precisão", "Unidade de Medida");
            Sheet limits = wb.createSheet("Limits");
            row(limits, 0, "ID", "Sigla do Output", "Tipo de Limite", "Valor Inferior", "Valor Superior",
                    "Valor de Atenção", "Valor de Alerta", "Valor de Emergência");
            wb.createSheet("Inputs");
            row(wb.getSheet("Inputs"), 0, "ID", "Sigla", "Nome", "Precisão", "Unidade de Medida");
            wb.createSheet("Constants");
            row(wb.getSheet("Constants"), 0, "ID", "Sigla", "Nome", "Precisão", "Valor", "Unidade de Medida");

            for (int i = 0; i < limitRows.length; i++) {
                String id = limitRows[i][0];
                row(instruments, i + 1, id, id, "PIEZOMETRO", "Margem direita");
                row(outputs, i + 1, id, limitRows[i][1], "Cota", "LEI", "2", "M");
                row(limits, i + 1, limitRows[i]);
            }

            wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void row(Sheet sheet, int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null || values[i].isEmpty()) {
                continue;
            }
            if (values[i].startsWith("=")) {
                row.createCell(i).setCellFormula(values[i].substring(1));
            } else {
                row.createCell(i).setCellValue(values[i]);
            }
        }
    }
}
