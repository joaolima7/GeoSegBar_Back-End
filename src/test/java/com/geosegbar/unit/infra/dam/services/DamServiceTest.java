package com.geosegbar.unit.infra.dam.services;

import java.util.Arrays;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.geosegbar.common.enums.StatusEnum;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("DamService Unit Tests - Simplified")
class DamServiceTest {

    @Mock
    private DamRepository damRepository;

    @Mock
    private StatusRepository statusRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private DamService service;

    private DamEntity damEntity;
    private StatusEntity statusEntity;
    private ClientEntity clientEntity;

    @BeforeEach
    void setUp() {
        clientEntity = new ClientEntity();
        clientEntity.setId(1L);
        clientEntity.setName("Test Client");

        statusEntity = new StatusEntity();
        statusEntity.setId(1L);
        statusEntity.setStatus(StatusEnum.ACTIVE);

        damEntity = new DamEntity();
        damEntity.setId(1L);
        damEntity.setName("Test Dam");
        damEntity.setClient(clientEntity);
        damEntity.setStatus(statusEntity);
    }

    @Test
    @DisplayName("Should find dam by ID successfully")
    void shouldFindDamByIdSuccessfully() {
        when(damRepository.findWithPsbFoldersById(1L)).thenReturn(Optional.of(damEntity));
        when(damRepository.findWithReservoirsById(1L)).thenReturn(Optional.of(damEntity));

        DamEntity result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(damRepository).findWithPsbFoldersById(1L);
        verify(damRepository).findWithReservoirsById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when dam not found")
    void shouldThrowNotFoundExceptionWhenDamNotFound() {
        when(damRepository.findWithPsbFoldersById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findById(1L));
    }

    @Test
    @DisplayName("Should find all dams")
    void shouldFindAllDams() {
        when(damRepository.findAll()).thenReturn(Arrays.asList(damEntity));
        when(damRepository.findWithPsbFoldersById(1L)).thenReturn(Optional.of(damEntity));
        when(damRepository.findWithReservoirsById(1L)).thenReturn(Optional.of(damEntity));

        List<DamEntity> results = service.findAll();

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(damRepository).findAll();
    }

    @Test
    @DisplayName("Should find dams by client ID")
    void shouldFindDamsByClientId() {
        when(damRepository.findWithPsbFoldersByClientId(1L)).thenReturn(Arrays.asList(damEntity));
        when(damRepository.findWithReservoirsByClientId(1L)).thenReturn(Arrays.asList(damEntity));

        List<DamEntity> results = service.findDamsByClientId(1L);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(damRepository).findWithPsbFoldersByClientId(1L);
        verify(damRepository).findWithReservoirsByClientId(1L);
    }

    @Test
    @DisplayName("Should check if dam exists by name")
    void shouldCheckIfDamExistsByName() {
        when(damRepository.existsByName("Test Dam")).thenReturn(true);

        boolean exists = service.existsByName("Test Dam");

        assertTrue(exists);
        verify(damRepository).existsByName("Test Dam");
    }

    @Test
    @DisplayName("Should check if dam exists by name and id not equal")
    void shouldCheckIfDamExistsByNameAndIdNot() {
        when(damRepository.existsByNameAndIdNot("Test Dam", 2L)).thenReturn(true);

        boolean exists = service.existsByNameAndIdNot("Test Dam", 2L);

        assertTrue(exists);
        verify(damRepository).existsByNameAndIdNot("Test Dam", 2L);
    }
}
