package com.geosegbar.unit.infra.constant.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.constant.persistence.jpa.ConstantRepository;
import com.geosegbar.infra.constant.services.ConstantService;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Unit tests for ConstantService")
public class ConstantServiceTest {

    @Mock
    private ConstantRepository constantRepository;

    @InjectMocks
    private ConstantService constantService;

    private ConstantEntity mockConstant;
    private InstrumentEntity mockInstrument;
    private MeasurementUnitEntity mockMeasurementUnit;

    @BeforeEach
    void setUp() {
        mockMeasurementUnit = new MeasurementUnitEntity();
        mockMeasurementUnit.setId(1L);
        mockMeasurementUnit.setName("Metros");
        mockMeasurementUnit.setAcronym("m");

        mockInstrument = new InstrumentEntity();
        mockInstrument.setId(1L);

        mockConstant = new ConstantEntity();
        mockConstant.setId(1L);
        mockConstant.setName("Constante K");
        mockConstant.setAcronym("K");
        mockConstant.setValue(1.5);
        mockConstant.setPrecision(2);
        mockConstant.setInstrument(mockInstrument);
        mockConstant.setMeasurementUnit(mockMeasurementUnit);
    }

    @Test
    @DisplayName("Should find constants by instrument ID successfully")
    void shouldFindConstantsByInstrumentIdSuccessfully() {
        // Given
        List<ConstantEntity> constants = Arrays.asList(mockConstant);
        when(constantRepository.findByInstrumentId(1L)).thenReturn(constants);

        // When
        List<ConstantEntity> result = constantService.findByInstrumentId(1L);

        // Then
        assertThat(result).hasSize(1);
        verify(constantRepository).findByInstrumentId(1L);
    }

    @Test
    @DisplayName("Should return empty list when no constants found for instrument")
    void shouldReturnEmptyListWhenNoConstantsFoundForInstrument() {
        // Given
        when(constantRepository.findByInstrumentId(1L)).thenReturn(Collections.emptyList());

        // When
        List<ConstantEntity> result = constantService.findByInstrumentId(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find constant by ID successfully")
    void shouldFindConstantByIdSuccessfully() {
        // Given
        when(constantRepository.findById(1L)).thenReturn(Optional.of(mockConstant));

        // When
        ConstantEntity result = constantService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when constant not found by ID")
    void shouldThrowNotFoundExceptionWhenConstantNotFoundById() {
        // Given
        when(constantRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> constantService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Constante não encontrada com ID: 99");
    }

    @Test
    @DisplayName("Should find constant IDs by instrument dam ID successfully")
    void shouldFindConstantIdsByInstrumentDamIdSuccessfully() {
        // Given
        List<Long> constantIds = Arrays.asList(1L, 2L, 3L);
        when(constantRepository.findConstantIdsByInstrumentDamId(1L)).thenReturn(constantIds);

        // When
        List<Long> result = constantService.findConstantIdsByInstrumentDamId(1L);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("Should return empty list when no constant IDs found for dam")
    void shouldReturnEmptyListWhenNoConstantIdsFoundForDam() {
        // Given
        when(constantRepository.findConstantIdsByInstrumentDamId(1L)).thenReturn(Collections.emptyList());

        // When
        List<Long> result = constantService.findConstantIdsByInstrumentDamId(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delete constant by ID successfully")
    void shouldDeleteConstantByIdSuccessfully() {
        // Given
        when(constantRepository.findById(1L)).thenReturn(Optional.of(mockConstant));

        // When
        constantService.deleteById(1L);

        // Then
        verify(constantRepository).findById(1L);
        verify(constantRepository).delete(mockConstant);
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting non-existent constant")
    void shouldThrowNotFoundExceptionWhenDeletingNonExistent() {
        // Given
        when(constantRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> constantService.deleteById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Constante não encontrada com ID: 99");
    }
}
