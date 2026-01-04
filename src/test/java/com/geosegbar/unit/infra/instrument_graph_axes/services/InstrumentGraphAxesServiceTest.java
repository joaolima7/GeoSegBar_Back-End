package com.geosegbar.unit.infra.instrument_graph_axes.services;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.entities.InstrumentGraphAxesEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.instrument_graph_axes.dtos.GraphAxesResponseDTO;
import com.geosegbar.infra.instrument_graph_axes.dtos.UpdateGraphAxesRequestDTO;
import com.geosegbar.infra.instrument_graph_axes.persistence.jpa.InstrumentGraphAxesRepository;
import com.geosegbar.infra.instrument_graph_axes.services.InstrumentGraphAxesService;
import com.geosegbar.infra.instrument_graph_pattern.services.InstrumentGraphPatternService;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("InstrumentGraphAxesService Unit Tests")
class InstrumentGraphAxesServiceTest {

    @Mock
    private InstrumentGraphAxesRepository axesRepository;

    @Mock
    private InstrumentGraphPatternService patternService;

    @InjectMocks
    private InstrumentGraphAxesService service;

    private InstrumentGraphAxesEntity axesEntity;
    private InstrumentGraphPatternEntity patternEntity;
    private UpdateGraphAxesRequestDTO updateRequest;

    @BeforeEach
    void setUp() {
        patternEntity = new InstrumentGraphPatternEntity();
        patternEntity.setId(1L);
        patternEntity.setName("Test Pattern");

        axesEntity = new InstrumentGraphAxesEntity();
        axesEntity.setId(1L);
        axesEntity.setPattern(patternEntity);
        axesEntity.setAbscissaPx(12);
        axesEntity.setAbscissaGridLinesEnable(true);
        axesEntity.setPrimaryOrdinatePx(14);
        axesEntity.setSecondaryOrdinatePx(14);
        axesEntity.setPrimaryOrdinateGridLinesEnable(true);
        axesEntity.setPrimaryOrdinateTitle("Primary Axis");
        axesEntity.setSecondaryOrdinateTitle("Secondary Axis");
        axesEntity.setPrimaryOrdinateSpacing(10.0);
        axesEntity.setSecondaryOrdinateSpacing(5.0);
        axesEntity.setPrimaryOrdinateInitialValue(0.0);
        axesEntity.setSecondaryOrdinateInitialValue(0.0);
        axesEntity.setPrimaryOrdinateMaximumValue(100.0);
        axesEntity.setSecondaryOrdinateMaximumValue(50.0);

        updateRequest = new UpdateGraphAxesRequestDTO();
        updateRequest.setAbscissaPx(16);
        updateRequest.setAbscissaGridLinesEnable(false);
        updateRequest.setPrimaryOrdinatePx(18);
        updateRequest.setSecondaryOrdinatePx(18);
        updateRequest.setPrimaryOrdinateGridLinesEnable(false);
        updateRequest.setPrimaryOrdinateTitle("Updated Primary");
        updateRequest.setSecondaryOrdinateTitle("Updated Secondary");
        updateRequest.setPrimaryOrdinateSpacing(20.0);
        updateRequest.setSecondaryOrdinateSpacing(10.0);
        updateRequest.setPrimaryOrdinateInitialValue(5.0);
        updateRequest.setSecondaryOrdinateInitialValue(2.0);
        updateRequest.setPrimaryOrdinateMaximumValue(200.0);
        updateRequest.setSecondaryOrdinateMaximumValue(100.0);
    }

    @Test
    @DisplayName("Should find axes by pattern ID successfully")
    void shouldFindAxesByPatternIdSuccessfully() {
        when(axesRepository.findByPatternId(1L)).thenReturn(Optional.of(axesEntity));

        InstrumentGraphAxesEntity result = service.findByPatternId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(12, result.getAbscissaPx());
        assertEquals(14, result.getPrimaryOrdinatePx());
        assertEquals("Primary Axis", result.getPrimaryOrdinateTitle());
        verify(axesRepository).findByPatternId(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when axes not found by pattern ID")
    void shouldThrowNotFoundExceptionWhenAxesNotFoundByPatternId() {
        when(axesRepository.findByPatternId(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service.findByPatternId(1L));

        assertTrue(exception.getMessage().contains("Eixos não encontrados para o padrão ID: 1"));
        verify(axesRepository).findByPatternId(1L);
    }

    @Test
    @DisplayName("Should update axes successfully")
    void shouldUpdateAxesSuccessfully() {
        when(patternService.findById(1L)).thenReturn(patternEntity);
        when(axesRepository.findByPatternId(1L)).thenReturn(Optional.of(axesEntity));
        when(axesRepository.save(any(InstrumentGraphAxesEntity.class))).thenReturn(axesEntity);

        GraphAxesResponseDTO result = service.updateAxes(1L, updateRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getPatternId());
        verify(patternService).findById(1L);
        verify(axesRepository).findByPatternId(1L);
        verify(axesRepository).save(axesEntity);
    }

    @Test
    @DisplayName("Should map entity to response DTO correctly")
    void shouldMapEntityToResponseDTOCorrectly() {
        GraphAxesResponseDTO result = service.mapToResponseDTO(axesEntity);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getPatternId());
        assertEquals(12, result.getAbscissaPx());
        assertTrue(result.getAbscissaGridLinesEnable());
        assertEquals(14, result.getPrimaryOrdinatePx());
        assertEquals(14, result.getSecondaryOrdinatePx());
        assertTrue(result.getPrimaryOrdinateGridLinesEnable());
        assertEquals("Primary Axis", result.getPrimaryOrdinateTitle());
        assertEquals("Secondary Axis", result.getSecondaryOrdinateTitle());
        assertEquals(10.0, result.getPrimaryOrdinateSpacing());
        assertEquals(5.0, result.getSecondaryOrdinateSpacing());
        assertEquals(0.0, result.getPrimaryOrdinateInitialValue());
        assertEquals(0.0, result.getSecondaryOrdinateInitialValue());
        assertEquals(100.0, result.getPrimaryOrdinateMaximumValue());
        assertEquals(50.0, result.getSecondaryOrdinateMaximumValue());
    }

    @Test
    @DisplayName("Should throw NotFoundException when pattern not found during update")
    void shouldThrowNotFoundExceptionWhenPatternNotFoundDuringUpdate() {
        when(patternService.findById(1L)).thenThrow(new NotFoundException("Pattern not found"));

        assertThrows(NotFoundException.class, () -> service.updateAxes(1L, updateRequest));
        verify(patternService).findById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when axes not found during update")
    void shouldThrowNotFoundExceptionWhenAxesNotFoundDuringUpdate() {
        when(patternService.findById(1L)).thenReturn(patternEntity);
        when(axesRepository.findByPatternId(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.updateAxes(1L, updateRequest));
        verify(patternService).findById(1L);
        verify(axesRepository).findByPatternId(1L);
    }
}
