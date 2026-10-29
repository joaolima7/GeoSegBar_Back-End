package com.geosegbar.unit.infra.reading;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InputEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.ReadingEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.dam.services.DamAccessService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;
import com.geosegbar.infra.reading.dtos.UpdateReadingRequestDTO;
import com.geosegbar.infra.reading.persistence.jpa.ReadingRepository;
import com.geosegbar.infra.reading.services.ReadingService;

/**
 * Todo input do instrumento exige valor numérico na leitura. Antes, um input
 * enviado como {@code null} passava pela validação (que só conferia a chave) e a
 * leitura era gravada sem ele quando a equação não chegava a usá-lo.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("ReadingService — valor obrigatório nos inputs")
class ReadingServiceInputValuesTest {

    @Mock
    private ReadingRepository readingRepository;
    @Mock
    private InstrumentRepository instrumentRepository;
    @Mock
    private DamAccessService damAccessService;

    @InjectMocks
    private ReadingService service;

    private MockedStatic<AuthenticatedUserUtil> auth;

    @BeforeEach
    void setUp() {
        auth = Mockito.mockStatic(AuthenticatedUserUtil.class);
        auth.when(AuthenticatedUserUtil::isAdmin).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        auth.close();
    }

    private InstrumentEntity piezometer() {
        DamEntity dam = new DamEntity();
        dam.setId(10L);
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setDam(dam);
        for (String acronym : new String[]{"MAN", "PIO"}) {
            InputEntity input = new InputEntity();
            input.setAcronym(acronym);
            input.setName(acronym);
            input.setInstrument(instrument);
            instrument.getInputs().add(input);
        }
        OutputEntity output = new OutputEntity();
        output.setAcronym("CP");
        output.setEquation("PIO > 0 ? (100 - PIO) : (90 + MAN * 10)");
        output.setActive(true);
        output.setInstrument(instrument);
        instrument.getOutputs().add(output);
        return instrument;
    }

    private static Map<String, Double> values(Double man, Double pio) {
        Map<String, Double> values = new HashMap<>();
        values.put("MAN", man);
        values.put("PIO", pio);
        return values;
    }

    @Test
    @DisplayName("registrar leitura com input nulo é recusado, mesmo que a equação não use o input")
    void createRejectsNullInput() {
        when(instrumentRepository.findWithActiveOutputsById(1L)).thenReturn(Optional.of(piezometer()));
        ReadingRequestDTO request = new ReadingRequestDTO(
                LocalDate.now().minusDays(1), LocalTime.of(9, 0), values(null, 3.5), null);

        assertThatThrownBy(() -> service.create(1L, request, false))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("'MAN'")
                .hasMessageContaining("informe 0");
        verify(readingRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("corrigir leitura apagando o valor de um input é recusado")
    void updateRejectsNullInput() {
        ReadingEntity reading = new ReadingEntity();
        reading.setId(50L);
        reading.setActive(true);
        reading.setDate(LocalDate.of(2026, 10, 1));
        reading.setHour(LocalTime.of(9, 0));
        reading.setInstrument(piezometer());
        when(readingRepository.findByIdWithAllRelations(50L)).thenReturn(Optional.of(reading));
        UpdateReadingRequestDTO request = new UpdateReadingRequestDTO();
        request.setInputValues(values(null, 3.5));

        assertThatThrownBy(() -> service.updateReading(50L, request))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("'MAN'");
        verify(readingRepository, never()).findAllReadingsInGroupWithRelations(anyLong(), any(), any());
    }
}
