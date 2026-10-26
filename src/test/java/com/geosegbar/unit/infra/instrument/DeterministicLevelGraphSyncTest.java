package com.geosegbar.unit.infra.instrument;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LimitValueTypeEnum;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentGraphCustomizationPropertiesEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.infra.instrument.services.InstrumentService;
import com.geosegbar.infra.instrument_graph_customization_properties.persistence.jpa.InstrumentGraphCustomizationPropertiesRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("InstrumentService - legendas dos níveis do limite determinístico")
class DeterministicLevelGraphSyncTest {

    @Mock
    private InstrumentGraphCustomizationPropertiesRepository graphCustomizationPropertiesRepository;

    @InjectMocks
    private InstrumentService service;

    private static InstrumentGraphCustomizationPropertiesEntity legend(InstrumentGraphPatternEntity pattern,
            DeterministicLimitEntity limit, LimitValueTypeEnum valueType) {
        InstrumentGraphCustomizationPropertiesEntity prop = new InstrumentGraphCustomizationPropertiesEntity();
        prop.setPattern(pattern);
        prop.setCustomizationType(CustomizationTypeEnum.DETERMINISTIC_LIMIT);
        prop.setDeterministicLimit(limit);
        prop.setLimitValueType(valueType);
        return prop;
    }

    private void sync(DeterministicLimitEntity limit, BigDecimal attention, BigDecimal alert, BigDecimal emergency) {
        ReflectionTestUtils.invokeMethod(service, "syncGraphPropertiesWithDeterministicLevels",
                limit, attention, alert, emergency);
    }

    @Test
    @DisplayName("remove a legenda do nível desligado e cria a do nível ligado nos padrões que já exibiam o limite")
    void removesDeactivatedAndAddsActivatedLevels() {
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setId(7L);
        limit.setAttentionValue(null);
        limit.setAlertValue(new BigDecimal("10"));
        limit.setEmergencyValue(new BigDecimal("12"));

        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setId(3L);
        InstrumentGraphCustomizationPropertiesEntity attention = legend(pattern, limit, LimitValueTypeEnum.DETERMINISTIC_ATTENTION);
        InstrumentGraphCustomizationPropertiesEntity alert = legend(pattern, limit, LimitValueTypeEnum.DETERMINISTIC_ALERT);
        when(graphCustomizationPropertiesRepository.findByDeterministicLimitId(7L)).thenReturn(List.of(attention, alert));

        sync(limit, new BigDecimal("8"), new BigDecimal("10"), null);

        verify(graphCustomizationPropertiesRepository).deleteAll(List.of(attention));
        ArgumentCaptor<InstrumentGraphCustomizationPropertiesEntity> created
                = ArgumentCaptor.forClass(InstrumentGraphCustomizationPropertiesEntity.class);
        verify(graphCustomizationPropertiesRepository).save(created.capture());
        assertThat(created.getValue().getPattern()).isSameAs(pattern);
        assertThat(created.getValue().getDeterministicLimit()).isSameAs(limit);
        assertThat(created.getValue().getLimitValueType()).isEqualTo(LimitValueTypeEnum.DETERMINISTIC_EMERGENCY);
        assertThat(created.getValue().getFillColor()).isEqualTo("#FF0000");
    }

    @Test
    @DisplayName("não mexe em nada quando nenhum padrão exibe o limite")
    void ignoresLimitWithoutLegends() {
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setId(7L);
        limit.setEmergencyValue(new BigDecimal("12"));
        when(graphCustomizationPropertiesRepository.findByDeterministicLimitId(7L)).thenReturn(List.of());

        sync(limit, null, null, null);

        verify(graphCustomizationPropertiesRepository, never()).deleteAll(anyList());
        verify(graphCustomizationPropertiesRepository, never()).save(any());
    }

    @Test
    @DisplayName("não cria legenda em padrão cuja legenda legada já desenha todos os níveis")
    void skipsPatternWithLegacyLegendForEveryLevel() {
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setId(7L);
        limit.setAlertValue(new BigDecimal("10"));
        limit.setEmergencyValue(new BigDecimal("12"));

        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setId(3L);
        InstrumentGraphCustomizationPropertiesEntity everyLevel = legend(pattern, limit, null);
        when(graphCustomizationPropertiesRepository.findByDeterministicLimitId(7L)).thenReturn(List.of(everyLevel));

        sync(limit, null, new BigDecimal("10"), null);

        verify(graphCustomizationPropertiesRepository, never()).deleteAll(anyList());
        verify(graphCustomizationPropertiesRepository, never()).save(any());
    }

    @Test
    @DisplayName("não duplica a legenda que o padrão já tem")
    void doesNotDuplicateExistingLegend() {
        DeterministicLimitEntity limit = new DeterministicLimitEntity();
        limit.setId(7L);
        limit.setAlertValue(new BigDecimal("10"));

        InstrumentGraphPatternEntity pattern = new InstrumentGraphPatternEntity();
        pattern.setId(3L);
        InstrumentGraphCustomizationPropertiesEntity alert = legend(pattern, limit, LimitValueTypeEnum.DETERMINISTIC_ALERT);
        when(graphCustomizationPropertiesRepository.findByDeterministicLimitId(7L)).thenReturn(List.of(alert));

        sync(limit, null, null, null);

        verify(graphCustomizationPropertiesRepository, never()).deleteAll(anyList());
        verify(graphCustomizationPropertiesRepository, never()).save(any());
    }
}
