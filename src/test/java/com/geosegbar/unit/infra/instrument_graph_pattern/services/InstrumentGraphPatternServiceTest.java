package com.geosegbar.unit.infra.instrument_graph_pattern.services;

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

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.InstrumentGraphPatternFolder;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.instrument_graph_axes.persistence.jpa.InstrumentGraphAxesRepository;
import com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternDetailResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern.persistence.jpa.InstrumentGraphPatternRepository;
import com.geosegbar.infra.instrument_graph_pattern.services.InstrumentGraphPatternService;
import com.geosegbar.infra.instrument_graph_pattern_folder.persistence.jpa.InstrumentGraphPatternFolderRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("InstrumentGraphPatternService Unit Tests")
class InstrumentGraphPatternServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private InstrumentGraphPatternRepository patternRepository;

    @Mock
    private InstrumentGraphAxesRepository axesRepository;

    @Mock
    private InstrumentGraphPatternFolderRepository folderRepository;

    @Mock
    private DamService damService;

    @InjectMocks
    private InstrumentGraphPatternService service;

    private InstrumentGraphPatternEntity patternEntity;
    private InstrumentEntity instrumentEntity;
    private InstrumentGraphPatternFolder folderEntity;
    private DamEntity damEntity;

    @BeforeEach
    void setUp() {
        damEntity = new DamEntity();
        damEntity.setId(1L);
        damEntity.setName("Test Dam");

        instrumentEntity = new InstrumentEntity();
        instrumentEntity.setId(1L);
        instrumentEntity.setName("Test Instrument");
        instrumentEntity.setDam(damEntity);

        folderEntity = new InstrumentGraphPatternFolder();
        folderEntity.setId(1L);
        folderEntity.setName("Test Folder");

        patternEntity = new InstrumentGraphPatternEntity();
        patternEntity.setId(1L);
        patternEntity.setName("Test Pattern");
        patternEntity.setInstrument(instrumentEntity);
        patternEntity.setFolder(folderEntity);
    }

    @Test
    @DisplayName("Should find patterns by instrument ID successfully")
    void shouldFindPatternsByInstrumentIdSuccessfully() {
        InstrumentGraphPatternEntity pattern2 = new InstrumentGraphPatternEntity();
        pattern2.setId(2L);
        pattern2.setName("Pattern 2");
        pattern2.setInstrument(instrumentEntity);

        when(patternRepository.findByInstrumentId(1L)).thenReturn(Arrays.asList(patternEntity, pattern2));

        List<GraphPatternResponseDTO> results = service.findByInstrument(1L);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Test Pattern", results.get(0).getName());
        assertEquals("Pattern 2", results.get(1).getName());
        verify(patternRepository).findByInstrumentId(1L);
    }

    @Test
    @DisplayName("Should return empty list when no patterns found for instrument")
    void shouldReturnEmptyListWhenNoPatternsFoundForInstrument() {
        when(patternRepository.findByInstrumentId(1L)).thenReturn(Collections.emptyList());

        List<GraphPatternResponseDTO> results = service.findByInstrument(1L);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(patternRepository).findByInstrumentId(1L);
    }

    @Test
    @DisplayName("Should find pattern by ID successfully")
    void shouldFindPatternByIdSuccessfully() {
        when(patternRepository.findById(1L)).thenReturn(Optional.of(patternEntity));

        InstrumentGraphPatternEntity result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Pattern", result.getName());
        verify(patternRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when pattern not found by ID")
    void shouldThrowNotFoundExceptionWhenPatternNotFoundById() {
        when(patternRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service.findById(1L));

        assertTrue(exception.getMessage().contains("Padrão de Gráfico não encontrado com ID: 1"));
        verify(patternRepository).findById(1L);
    }

    @Test
    @DisplayName("Should find patterns by instrument ID with details successfully")
    void shouldFindPatternsByInstrumentIdWithDetailsSuccessfully() {
        when(patternRepository.findByInstrumentIdWithAllDetails(1L)).thenReturn(Arrays.asList(patternEntity));

        List<GraphPatternDetailResponseDTO> results = service.findByInstrumentWithDetails(1L);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(patternRepository).findByInstrumentIdWithAllDetails(1L);
    }

    @Test
    @DisplayName("Should find all patterns by dam ID successfully")
    void shouldFindAllPatternsByDamIdSuccessfully() {
        when(damService.findById(1L)).thenReturn(damEntity);
        when(patternRepository.findByInstrumentDamIdWithAllDetails(1L)).thenReturn(Arrays.asList(patternEntity));

        List<GraphPatternDetailResponseDTO> results = service.findAllPatternsByDam(1L);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(damService).findById(1L);
        verify(patternRepository).findByInstrumentDamIdWithAllDetails(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when dam not found")
    void shouldThrowNotFoundExceptionWhenDamNotFound() {
        when(damService.findById(1L)).thenThrow(new NotFoundException("Barragem não encontrada!"));

        assertThrows(NotFoundException.class, () -> service.findAllPatternsByDam(1L));
        verify(damService).findById(1L);
    }
}
