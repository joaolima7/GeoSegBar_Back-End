package com.geosegbar.unit.infra.instrument.services;

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

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.instrument.dtos.InstrumentResponseDTO;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.instrument.services.InstrumentService;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("InstrumentService Unit Tests - Simplified")
class InstrumentServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks
    private InstrumentService service;

    private InstrumentEntity instrumentEntity;
    private DamEntity damEntity;
    private InstrumentTypeEntity instrumentTypeEntity;

    @BeforeEach
    void setUp() {
        damEntity = new DamEntity();
        damEntity.setId(1L);
        damEntity.setName("Test Dam");

        instrumentTypeEntity = new InstrumentTypeEntity();
        instrumentTypeEntity.setId(1L);
        instrumentTypeEntity.setName("Piezômetro");

        instrumentEntity = new InstrumentEntity();
        instrumentEntity.setId(1L);
        instrumentEntity.setName("Test Instrument");
        instrumentEntity.setLocation("Test Location");
        instrumentEntity.setLatitude(-23.5505);
        instrumentEntity.setLongitude(-46.6333);
        instrumentEntity.setNoLimit(false);
        instrumentEntity.setActive(true);
        instrumentEntity.setActiveForSection(true);
        instrumentEntity.setIsLinimetricRuler(false);
        instrumentEntity.setDam(damEntity);
        instrumentEntity.setInstrumentType(instrumentTypeEntity);
    }

    @Test
    @DisplayName("Should find instrument by ID successfully with findById")
    void shouldFindInstrumentByIdSuccessfully() {
        when(instrumentRepository.findByIdWithBasicRelations(1L)).thenReturn(Optional.of(instrumentEntity));

        InstrumentEntity result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Instrument", result.getName());
        assertEquals("Test Location", result.getLocation());
        verify(instrumentRepository).findByIdWithBasicRelations(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when instrument not found by ID")
    void shouldThrowNotFoundExceptionWhenInstrumentNotFoundById() {
        when(instrumentRepository.findByIdWithBasicRelations(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service.findById(1L));

        assertTrue(exception.getMessage().contains("Instrumento não encontrado com ID: 1"));
        verify(instrumentRepository).findByIdWithBasicRelations(1L);
    }

    @Test
    @DisplayName("Should find instrument by ID and return DTO with findByIdDTO")
    void shouldFindInstrumentByIdAndReturnDTO() {
        when(instrumentRepository.findByIdWithBasicRelations(1L)).thenReturn(Optional.of(instrumentEntity));

        InstrumentResponseDTO result = service.findByIdDTO(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Instrument", result.getName());
        assertEquals("Test Location", result.getLocation());
        assertEquals(-23.5505, result.getLatitude());
        assertEquals(-46.6333, result.getLongitude());
        assertEquals(1L, result.getDamId());
        assertEquals("Test Dam", result.getDamName());
        assertEquals(1L, result.getInstrumentTypeId());
        assertEquals("Piezômetro", result.getInstrumentType());
        verify(instrumentRepository).findByIdWithBasicRelations(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when instrument not found by ID with findByIdDTO")
    void shouldThrowNotFoundExceptionWhenInstrumentNotFoundByIdDTO() {
        when(instrumentRepository.findByIdWithBasicRelations(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service.findByIdDTO(1L));

        assertTrue(exception.getMessage().contains("Instrumento não encontrado com ID: 1"));
        verify(instrumentRepository).findByIdWithBasicRelations(1L);
    }
}
