package com.geosegbar.unit.infra.instrument_graph_pattern_folder.services;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.InstrumentGraphPatternFolder;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.instrument_graph_pattern.persistence.jpa.InstrumentGraphPatternRepository;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.CreateFolderRequestDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.FolderDetailResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.FolderResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.UpdateFolderRequestDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.persistence.jpa.InstrumentGraphPatternFolderRepository;
import com.geosegbar.infra.instrument_graph_pattern_folder.services.InstrumentGraphPatternFolderService;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class InstrumentGraphPatternFolderServiceTest {

    @Mock
    private InstrumentGraphPatternFolderRepository folderRepository;

    @Mock
    private InstrumentGraphPatternRepository patternRepository;

    @Mock
    private DamService damService;

    @InjectMocks
    private InstrumentGraphPatternFolderService service;

    private InstrumentGraphPatternFolder folderEntity;
    private DamEntity damEntity;
    private InstrumentGraphPatternEntity patternEntity;
    private InstrumentEntity instrumentEntity;

    @BeforeEach
    void setUp() {
        damEntity = new DamEntity();
        damEntity.setId(1L);
        damEntity.setName("Test Dam");

        instrumentEntity = new InstrumentEntity();
        instrumentEntity.setId(1L);
        instrumentEntity.setName("Test Instrument");

        folderEntity = new InstrumentGraphPatternFolder();
        folderEntity.setId(1L);
        folderEntity.setName("Test Folder");
        folderEntity.setDam(damEntity);

        patternEntity = new InstrumentGraphPatternEntity();
        patternEntity.setId(1L);
        patternEntity.setName("Test Pattern");
        patternEntity.setFolder(folderEntity);
        patternEntity.setInstrument(instrumentEntity);
    }

    @Test
    void shouldCreateFolderSuccessfully() {
        CreateFolderRequestDTO requestDTO = new CreateFolderRequestDTO();
        requestDTO.setName("New Folder");
        requestDTO.setDamId(1L);

        when(folderRepository.existsByNameAndDamId("New Folder", 1L)).thenReturn(false);
        when(damService.findById(1L)).thenReturn(damEntity);
        when(folderRepository.save(any(InstrumentGraphPatternFolder.class))).thenAnswer(invocation -> {
            InstrumentGraphPatternFolder folder = invocation.getArgument(0);
            folder.setId(2L);
            return folder;
        });

        FolderResponseDTO result = service.create(requestDTO);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("New Folder", result.getName());
        assertNotNull(result.getDam());
        assertEquals(1L, result.getDam().getId());

        verify(folderRepository).existsByNameAndDamId("New Folder", 1L);
        verify(damService).findById(1L);
        verify(folderRepository).save(any(InstrumentGraphPatternFolder.class));
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenCreatingFolderWithExistingName() {
        CreateFolderRequestDTO requestDTO = new CreateFolderRequestDTO();
        requestDTO.setName("Test Folder");
        requestDTO.setDamId(1L);

        when(folderRepository.existsByNameAndDamId("Test Folder", 1L)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            service.create(requestDTO);
        });

        assertEquals("Já existe uma pasta com o nome 'Test Folder' nesta barragem!", exception.getMessage());
        verify(folderRepository).existsByNameAndDamId("Test Folder", 1L);
        verify(folderRepository, never()).save(any());
    }

    @Test
    void shouldUpdateFolderSuccessfully() {
        UpdateFolderRequestDTO requestDTO = new UpdateFolderRequestDTO();
        requestDTO.setName("Updated Folder");
        requestDTO.setPatternIds(Arrays.asList());

        when(folderRepository.findById(1L)).thenReturn(Optional.of(folderEntity));
        when(folderRepository.existsByNameAndDamId("Updated Folder", 1L)).thenReturn(false);
        when(folderRepository.save(any(InstrumentGraphPatternFolder.class))).thenReturn(folderEntity);

        FolderResponseDTO result = service.update(1L, requestDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Updated Folder", result.getName());

        verify(folderRepository).findById(1L);
        verify(folderRepository).existsByNameAndDamId("Updated Folder", 1L);
        verify(folderRepository).save(folderEntity);
    }

    @Test
    void shouldThrowDuplicateResourceExceptionWhenUpdatingWithExistingName() {
        UpdateFolderRequestDTO requestDTO = new UpdateFolderRequestDTO();
        requestDTO.setName("Another Folder");
        requestDTO.setPatternIds(Arrays.asList());

        when(folderRepository.findById(1L)).thenReturn(Optional.of(folderEntity));
        when(folderRepository.existsByNameAndDamId("Another Folder", 1L)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            service.update(1L, requestDTO);
        });

        assertEquals("Já existe uma pasta com o nome 'Another Folder' nesta barragem!", exception.getMessage());
        verify(folderRepository).findById(1L);
        verify(folderRepository).existsByNameAndDamId("Another Folder", 1L);
        verify(folderRepository, never()).save(any());
    }

    @Test
    void shouldDeleteFolderSuccessfully() {
        List<InstrumentGraphPatternEntity> patterns = Arrays.asList(patternEntity);

        when(folderRepository.findById(1L)).thenReturn(Optional.of(folderEntity));
        when(patternRepository.findByFolderId(1L)).thenReturn(patterns);

        service.delete(1L);

        verify(folderRepository).findById(1L);
        verify(patternRepository).findByFolderId(1L);
        verify(patternRepository).saveAll(patterns);
        verify(folderRepository).delete(folderEntity);

        assertNull(patternEntity.getFolder());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDeletingNonExistentFolder() {
        when(folderRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            service.delete(999L);
        });

        assertEquals("Pasta não encontrada com ID: 999", exception.getMessage());
        verify(folderRepository).findById(999L);
        verify(folderRepository, never()).delete(any());
    }

    @Test
    void shouldFindByIdSuccessfully() {
        when(folderRepository.findById(1L)).thenReturn(Optional.of(folderEntity));

        InstrumentGraphPatternFolder result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Folder", result.getName());
        assertEquals(1L, result.getDam().getId());

        verify(folderRepository).findById(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenFolderNotFoundById() {
        when(folderRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            service.findById(999L);
        });

        assertEquals("Pasta não encontrada com ID: 999", exception.getMessage());
        verify(folderRepository).findById(999L);
    }

    @Test
    void shouldFindByIdSimpleSuccessfully() {
        when(folderRepository.findById(1L)).thenReturn(Optional.of(folderEntity));

        FolderResponseDTO result = service.findByIdSimple(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Folder", result.getName());
        assertNotNull(result.getDam());
        assertEquals(1L, result.getDam().getId());

        verify(folderRepository).findById(1L);
    }

    @Test
    void shouldFindByIdWithDetailsSuccessfully() {
        when(folderRepository.findByIdWithDam(1L)).thenReturn(Optional.of(folderEntity));

        FolderDetailResponseDTO result = service.findByIdWithDetails(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Folder", result.getName());
        assertNotNull(result.getDam());
        assertEquals(1L, result.getDam().getId());
        assertEquals("Test Dam", result.getDam().getName());

        verify(folderRepository).findByIdWithDam(1L);
    }
}
