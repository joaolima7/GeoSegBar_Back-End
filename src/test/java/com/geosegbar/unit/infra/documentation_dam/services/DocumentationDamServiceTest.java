package com.geosegbar.unit.infra.documentation_dam.services;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DocumentationDamEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.documentation_dam.dtos.DocumentationDamDTO;
import com.geosegbar.infra.documentation_dam.persistence.DocumentationDamRepository;
import com.geosegbar.infra.documentation_dam.services.DocumentationDamService;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("DocumentationDamService Unit Tests")
class DocumentationDamServiceTest {

    @Mock
    private DocumentationDamRepository documentationDamRepository;

    @Mock
    private DamRepository damRepository;

    @InjectMocks
    private DocumentationDamService service;

    private DocumentationDamEntity documentationEntity;
    private DamEntity damEntity;
    private DocumentationDamDTO documentationDTO;

    @BeforeEach
    void setUp() {
        damEntity = new DamEntity();
        damEntity.setId(1L);
        damEntity.setName("Test Dam");

        documentationEntity = new DocumentationDamEntity();
        documentationEntity.setId(1L);
        documentationEntity.setDam(damEntity);
        documentationEntity.setLastUpdatePAE(LocalDate.of(2024, 1, 1));
        documentationEntity.setNextUpdatePAE(LocalDate.of(2025, 1, 1));

        documentationDTO = new DocumentationDamDTO();
        documentationDTO.setDamId(1L);
        documentationDTO.setLastUpdatePAE(LocalDate.of(2024, 1, 1));
        documentationDTO.setNextUpdatePAE(LocalDate.of(2025, 1, 1));
    }

    @Test
    @DisplayName("Should find documentation by ID successfully")
    void shouldFindDocumentationByIdSuccessfully() {
        when(documentationDamRepository.findById(1L)).thenReturn(Optional.of(documentationEntity));

        DocumentationDamEntity result = service.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(LocalDate.of(2024, 1, 1), result.getLastUpdatePAE());
        verify(documentationDamRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when documentation not found by ID")
    void shouldThrowNotFoundExceptionWhenDocumentationNotFoundById() {
        when(documentationDamRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> service.findById(1L));

        assertTrue(exception.getMessage().contains("Documentação de barragem não encontrada com ID: 1"));
        verify(documentationDamRepository).findById(1L);
    }

    @Test
    @DisplayName("Should find documentation by dam ID successfully")
    void shouldFindDocumentationByDamIdSuccessfully() {
        when(documentationDamRepository.findByDamId(1L)).thenReturn(Optional.of(documentationEntity));

        DocumentationDamEntity result = service.findByDamId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getDam().getId());
        verify(documentationDamRepository).findByDamId(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when documentation not found by dam ID")
    void shouldThrowNotFoundExceptionWhenDocumentationNotFoundByDamId() {
        when(documentationDamRepository.findByDamId(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> service.findByDamId(1L));

        assertTrue(exception.getMessage().contains("Documentação não encontrada para a barragem com ID: 1"));
        verify(documentationDamRepository).findByDamId(1L);
    }

    @Test
    @DisplayName("Should find all documentations successfully")
    void shouldFindAllDocumentationsSuccessfully() {
        DocumentationDamEntity doc2 = new DocumentationDamEntity();
        doc2.setId(2L);

        when(documentationDamRepository.findAll()).thenReturn(Arrays.asList(documentationEntity, doc2));

        List<DocumentationDamEntity> results = service.findAll();

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(documentationDamRepository).findAll();
    }

    @Test
    @DisplayName("Should create new documentation successfully")
    void shouldCreateNewDocumentationSuccessfully() {
        when(damRepository.findById(1L)).thenReturn(Optional.of(damEntity));
        when(documentationDamRepository.existsByDam(damEntity)).thenReturn(false);
        when(documentationDamRepository.save(any(DocumentationDamEntity.class))).thenReturn(documentationEntity);

        DocumentationDamEntity result = service.createOrUpdate(documentationDTO);

        assertNotNull(result);
        verify(damRepository).findById(1L);
        verify(documentationDamRepository).existsByDam(damEntity);
        verify(documentationDamRepository).save(any(DocumentationDamEntity.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when creating documentation for non-existent dam")
    void shouldThrowNotFoundExceptionWhenCreatingForNonExistentDam() {
        when(damRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service.createOrUpdate(documentationDTO));

        assertTrue(exception.getMessage().contains("Barragem não encontrada com ID: 1"));
        verify(damRepository).findById(1L);
        verify(documentationDamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when documentation already exists for dam")
    void shouldThrowDuplicateExceptionWhenDocumentationAlreadyExists() {
        when(damRepository.findById(1L)).thenReturn(Optional.of(damEntity));
        when(documentationDamRepository.existsByDam(damEntity)).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> service.createOrUpdate(documentationDTO));

        assertTrue(exception.getMessage().contains("Já existe documentação para esta barragem"));
        verify(documentationDamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update existing documentation successfully")
    void shouldUpdateExistingDocumentationSuccessfully() {
        documentationDTO.setId(1L);
        documentationDTO.setLastUpdatePSB(LocalDate.of(2024, 2, 1));

        when(damRepository.findById(1L)).thenReturn(Optional.of(damEntity));
        when(documentationDamRepository.findById(1L)).thenReturn(Optional.of(documentationEntity));
        when(documentationDamRepository.save(any(DocumentationDamEntity.class))).thenReturn(documentationEntity);

        DocumentationDamEntity result = service.createOrUpdate(documentationDTO);

        assertNotNull(result);
        verify(documentationDamRepository).findById(1L);
        verify(documentationDamRepository).save(any(DocumentationDamEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when trying to change dam association")
    void shouldThrowExceptionWhenChangingDamAssociation() {
        documentationDTO.setId(1L);
        documentationDTO.setDamId(2L); // Different dam ID

        when(damRepository.findById(2L)).thenReturn(Optional.of(damEntity));
        when(documentationDamRepository.findById(1L)).thenReturn(Optional.of(documentationEntity));

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class,
                () -> service.createOrUpdate(documentationDTO));

        assertTrue(exception.getMessage().contains("Não é permitido mudar a barragem associada à documentação"));
        verify(documentationDamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete documentation and clear dam reference")
    void shouldDeleteDocumentationAndClearDamReference() {
        damEntity.setDocumentationDam(documentationEntity);

        when(documentationDamRepository.findById(1L)).thenReturn(Optional.of(documentationEntity));
        when(damRepository.save(any(DamEntity.class))).thenReturn(damEntity);

        service.delete(1L);

        verify(documentationDamRepository).findById(1L);
        verify(damRepository).save(damEntity);
        verify(documentationDamRepository).delete(documentationEntity);
        assertNull(damEntity.getDocumentationDam());
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting non-existent documentation")
    void shouldThrowNotFoundExceptionWhenDeletingNonExistent() {
        when(documentationDamRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> service.delete(1L));

        assertTrue(exception.getMessage().contains("Documentação de barragem não encontrada com ID: 1"));
        verify(documentationDamRepository).findById(1L);
        verify(documentationDamRepository, never()).delete(any());
    }
}
