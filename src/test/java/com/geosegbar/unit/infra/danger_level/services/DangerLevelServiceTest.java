package com.geosegbar.unit.infra.danger_level.services;

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

import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.danger_level.persistence.jpa.DangerLevelRepository;
import com.geosegbar.infra.danger_level.services.DangerLevelService;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("DangerLevelService Unit Tests")
class DangerLevelServiceTest {

    @Mock
    private DangerLevelRepository dangerLevelRepository;

    @InjectMocks
    private DangerLevelService service;

    private DangerLevelEntity dangerLevelEntity;

    @BeforeEach
    void setUp() {
        dangerLevelEntity = new DangerLevelEntity();
        dangerLevelEntity.setId(1L);
        dangerLevelEntity.setName("Normal");
        dangerLevelEntity.setDescription("Condições normais de operação");
    }

    @Test
    @DisplayName("Should find all danger levels successfully")
    void shouldFindAllDangerLevelsSuccessfully() {
        DangerLevelEntity level2 = new DangerLevelEntity();
        level2.setId(2L);
        level2.setName("Atenção");
        level2.setDescription("Anomalia que requer atenção");

        when(dangerLevelRepository.findAll()).thenReturn(Arrays.asList(dangerLevelEntity, level2));

        List<DangerLevelEntity> results = service.findAll();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Normal", results.get(0).getName());
        assertEquals("Atenção", results.get(1).getName());
        verify(dangerLevelRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no danger levels found")
    void shouldReturnEmptyListWhenNoDangerLevelsFound() {
        when(dangerLevelRepository.findAll()).thenReturn(Collections.emptyList());

        List<DangerLevelEntity> results = service.findAll();

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(dangerLevelRepository).findAll();
    }

    @Test
    @DisplayName("Should find danger level by ID successfully")
    void shouldFindDangerLevelByIdSuccessfully() {
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(dangerLevelEntity));

        DangerLevelEntity result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Normal", result.getName());
        assertEquals("Condições normais de operação", result.getDescription());
        verify(dangerLevelRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when danger level not found by ID")
    void shouldThrowNotFoundExceptionWhenDangerLevelNotFoundById() {
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> service.findById(1L));

        assertEquals("Danger level not found!", exception.getMessage());
        verify(dangerLevelRepository).findById(1L);
    }

    @Test
    @DisplayName("Should find danger level by name successfully")
    void shouldFindDangerLevelByNameSuccessfully() {
        when(dangerLevelRepository.findByName("Normal")).thenReturn(Optional.of(dangerLevelEntity));

        DangerLevelEntity result = service.findByName("Normal");

        assertNotNull(result);
        assertEquals("Normal", result.getName());
        assertEquals("Condições normais de operação", result.getDescription());
        verify(dangerLevelRepository).findByName("Normal");
    }

    @Test
    @DisplayName("Should throw NotFoundException when danger level not found by name")
    void shouldThrowNotFoundExceptionWhenDangerLevelNotFoundByName() {
        when(dangerLevelRepository.findByName("Inexistente")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> service.findByName("Inexistente"));

        assertTrue(exception.getMessage().contains("Danger level not found with name: Inexistente"));
        verify(dangerLevelRepository).findByName("Inexistente");
    }
}
