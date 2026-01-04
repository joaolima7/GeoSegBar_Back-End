package com.geosegbar.unit.infra.input.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.entities.InputEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.input.persistence.jpa.InputRepository;
import com.geosegbar.infra.input.services.InputService;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("InputService Unit Tests")
class InputServiceTest {

    @Mock
    private InputRepository inputRepository;

    @InjectMocks
    private InputService service;

    private InputEntity inputEntity;
    private InstrumentEntity instrumentEntity;
    private MeasurementUnitEntity measurementUnitEntity;

    @BeforeEach
    void setUp() {
        measurementUnitEntity = new MeasurementUnitEntity();
        measurementUnitEntity.setId(1L);
        measurementUnitEntity.setName("Metro");
        measurementUnitEntity.setAcronym("m");

        instrumentEntity = new InstrumentEntity();
        instrumentEntity.setId(1L);
        instrumentEntity.setName("Test Instrument");

        inputEntity = new InputEntity();
        inputEntity.setId(1L);
        inputEntity.setName("Test Input");
        inputEntity.setAcronym("TI");
        inputEntity.setPrecision(2);
        inputEntity.setInstrument(instrumentEntity);
        inputEntity.setMeasurementUnit(measurementUnitEntity);
    }

    @Test
    @DisplayName("Should find inputs by instrument ID successfully")
    void shouldFindInputsByInstrumentIdSuccessfully() {
        InputEntity input2 = new InputEntity();
        input2.setId(2L);
        input2.setName("Test Input 2");
        input2.setInstrument(instrumentEntity);

        when(inputRepository.findByInstrumentId(1L)).thenReturn(Arrays.asList(inputEntity, input2));

        List<InputEntity> results = service.findByInstrumentId(1L);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Test Input", results.get(0).getName());
        assertEquals("Test Input 2", results.get(1).getName());
        verify(inputRepository).findByInstrumentId(1L);
    }

    @Test
    @DisplayName("Should return empty list when no inputs found for instrument")
    void shouldReturnEmptyListWhenNoInputsFoundForInstrument() {
        when(inputRepository.findByInstrumentId(1L)).thenReturn(Collections.emptyList());

        List<InputEntity> results = service.findByInstrumentId(1L);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(inputRepository).findByInstrumentId(1L);
    }

    @Test
    @DisplayName("Should find input by ID successfully")
    void shouldFindInputByIdSuccessfully() {
        when(inputRepository.findById(1L)).thenReturn(Optional.of(inputEntity));

        InputEntity result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Input", result.getName());
        assertEquals("TI", result.getAcronym());
        assertEquals(2, result.getPrecision());
        verify(inputRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when input not found by ID")
    void shouldThrowNotFoundExceptionWhenInputNotFoundById() {
        when(inputRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service.findById(1L));

        assertTrue(exception.getMessage().contains("Input não encontrado com ID: 1"));
        verify(inputRepository).findById(1L);
    }

    @Test
    @DisplayName("Should delete input by ID successfully")
    void shouldDeleteInputByIdSuccessfully() {
        when(inputRepository.findById(1L)).thenReturn(Optional.of(inputEntity));

        service.deleteById(1L);

        verify(inputRepository).findById(1L);
        verify(inputRepository).delete(inputEntity);
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting non-existent input")
    void shouldThrowNotFoundExceptionWhenDeletingNonExistentInput() {
        when(inputRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.deleteById(1L));
        verify(inputRepository).findById(1L);
    }
}
