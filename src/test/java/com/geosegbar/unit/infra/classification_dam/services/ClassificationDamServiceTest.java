package com.geosegbar.unit.infra.classification_dam.services;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClassificationDamEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.classification_dam.peristence.ClassificationDamRepository;
import com.geosegbar.infra.classification_dam.services.ClassificationDamService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassificationDamService Unit Tests")
class ClassificationDamServiceTest extends BaseUnitTest {

    @Mock
    private ClassificationDamRepository classificationDamRepository;

    @InjectMocks
    private ClassificationDamService classificationDamService;

    private ClassificationDamEntity mockClassification;

    @BeforeEach
    void setUp() {
        mockClassification = new ClassificationDamEntity();
        mockClassification.setId(1L);
        mockClassification.setClassification("A");
    }

    // ==================== @PostConstruct Initialization Tests ====================
    @Test
    @DisplayName("Should initialize all default classifications A-E on startup")
    void shouldInitializeDefaultClassifications() {
        // Given
        when(classificationDamRepository.existsByClassification("A")).thenReturn(false);
        when(classificationDamRepository.existsByClassification("B")).thenReturn(false);
        when(classificationDamRepository.existsByClassification("C")).thenReturn(false);
        when(classificationDamRepository.existsByClassification("D")).thenReturn(false);
        when(classificationDamRepository.existsByClassification("E")).thenReturn(false);
        when(classificationDamRepository.save(any(ClassificationDamEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        classificationDamService.initializeDefaultClassifications();

        // Then
        verify(classificationDamRepository, times(5)).save(any(ClassificationDamEntity.class));
        verify(classificationDamRepository).existsByClassification("A");
        verify(classificationDamRepository).existsByClassification("B");
        verify(classificationDamRepository).existsByClassification("C");
        verify(classificationDamRepository).existsByClassification("D");
        verify(classificationDamRepository).existsByClassification("E");
    }

    @Test
    @DisplayName("Should not create duplicate classifications when they already exist")
    void shouldNotCreateDuplicateClassificationsOnInit() {
        // Given
        when(classificationDamRepository.existsByClassification("A")).thenReturn(true);
        when(classificationDamRepository.existsByClassification("B")).thenReturn(false);
        when(classificationDamRepository.existsByClassification("C")).thenReturn(true);
        when(classificationDamRepository.existsByClassification("D")).thenReturn(false);
        when(classificationDamRepository.existsByClassification("E")).thenReturn(true);
        when(classificationDamRepository.save(any(ClassificationDamEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        classificationDamService.initializeDefaultClassifications();

        // Then
        verify(classificationDamRepository, times(2)).save(any(ClassificationDamEntity.class));
        verify(classificationDamRepository, never()).save(argThat(entity -> "A".equals(entity.getClassification())));
        verify(classificationDamRepository, never()).save(argThat(entity -> "C".equals(entity.getClassification())));
        verify(classificationDamRepository, never()).save(argThat(entity -> "E".equals(entity.getClassification())));
    }

    @Test
    @DisplayName("Should be idempotent - multiple calls should not create duplicates")
    void shouldBeIdempotentInitialization() {
        // Given - First call: all return false (none exist)
        when(classificationDamRepository.existsByClassification("A")).thenReturn(false);
        when(classificationDamRepository.existsByClassification("B")).thenReturn(false);
        when(classificationDamRepository.existsByClassification("C")).thenReturn(false);
        when(classificationDamRepository.existsByClassification("D")).thenReturn(false);
        when(classificationDamRepository.existsByClassification("E")).thenReturn(false);
        when(classificationDamRepository.save(any(ClassificationDamEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When - First call creates all
        classificationDamService.initializeDefaultClassifications();

        // Then - 5 classifications created
        verify(classificationDamRepository, times(5)).save(any(ClassificationDamEntity.class));

        // Given - Second call: all return true (all exist)
        reset(classificationDamRepository);
        when(classificationDamRepository.existsByClassification(anyString())).thenReturn(true);

        // When - Second call should not create any
        classificationDamService.initializeDefaultClassifications();

        // Then - No additional saves
        verify(classificationDamRepository, never()).save(any(ClassificationDamEntity.class));
    }

    // ==================== Save Tests ====================
    @Test
    @DisplayName("Should save classification successfully")
    void shouldSaveClassificationSuccessfully() {
        // Given
        when(classificationDamRepository.existsByClassification("A")).thenReturn(false);
        when(classificationDamRepository.save(mockClassification)).thenReturn(mockClassification);

        // When
        ClassificationDamEntity result = classificationDamService.save(mockClassification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getClassification()).isEqualTo("A");
        verify(classificationDamRepository).existsByClassification("A");
        verify(classificationDamRepository).save(mockClassification);
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when classification already exists")
    void shouldThrowDuplicateResourceExceptionWhenClassificationExists() {
        // Given
        when(classificationDamRepository.existsByClassification("A")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> classificationDamService.save(mockClassification))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Já existe uma classificação de barragem com este nome!");

        verify(classificationDamRepository).existsByClassification("A");
        verify(classificationDamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should save classification with single character")
    void shouldSaveClassificationWithSingleCharacter() {
        // Given
        ClassificationDamEntity singleChar = new ClassificationDamEntity();
        singleChar.setClassification("F");
        when(classificationDamRepository.existsByClassification("F")).thenReturn(false);
        when(classificationDamRepository.save(singleChar)).thenReturn(singleChar);

        // When
        ClassificationDamEntity result = classificationDamService.save(singleChar);

        // Then
        assertThat(result.getClassification()).isEqualTo("F");
        verify(classificationDamRepository).save(singleChar);
    }

    // ==================== Update Tests ====================
    @Test
    @DisplayName("Should update classification successfully")
    void shouldUpdateClassificationSuccessfully() {
        // Given
        mockClassification.setClassification("B");
        when(classificationDamRepository.findById(1L)).thenReturn(Optional.of(mockClassification));
        when(classificationDamRepository.existsByClassificationAndIdNot("B", 1L)).thenReturn(false);
        when(classificationDamRepository.save(mockClassification)).thenReturn(mockClassification);

        // When
        ClassificationDamEntity result = classificationDamService.update(mockClassification);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getClassification()).isEqualTo("B");
        verify(classificationDamRepository).findById(1L);
        verify(classificationDamRepository).existsByClassificationAndIdNot("B", 1L);
        verify(classificationDamRepository).save(mockClassification);
    }

    @Test
    @DisplayName("Should throw NotFoundException when updating non-existent classification")
    void shouldThrowNotFoundExceptionWhenUpdatingNonExistent() {
        // Given
        when(classificationDamRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> classificationDamService.update(mockClassification))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Classificação de barragem não encontrada para atualização!");

        verify(classificationDamRepository).findById(1L);
        verify(classificationDamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when updating to existing classification name")
    void shouldThrowDuplicateResourceExceptionWhenUpdatingToExistingName() {
        // Given
        mockClassification.setClassification("C");
        when(classificationDamRepository.findById(1L)).thenReturn(Optional.of(mockClassification));
        when(classificationDamRepository.existsByClassificationAndIdNot("C", 1L)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> classificationDamService.update(mockClassification))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Já existe uma classificação de barragem com este nome!");

        verify(classificationDamRepository).findById(1L);
        verify(classificationDamRepository).existsByClassificationAndIdNot("C", 1L);
        verify(classificationDamRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allow update to same classification name")
    void shouldAllowUpdateToSameClassificationName() {
        // Given
        when(classificationDamRepository.findById(1L)).thenReturn(Optional.of(mockClassification));
        when(classificationDamRepository.existsByClassificationAndIdNot("A", 1L)).thenReturn(false);
        when(classificationDamRepository.save(mockClassification)).thenReturn(mockClassification);

        // When
        ClassificationDamEntity result = classificationDamService.update(mockClassification);

        // Then
        assertThat(result.getClassification()).isEqualTo("A");
        verify(classificationDamRepository).save(mockClassification);
    }

    // ==================== Delete Tests ====================
    @Test
    @DisplayName("Should delete classification successfully")
    void shouldDeleteClassificationSuccessfully() {
        // Given
        when(classificationDamRepository.findById(1L)).thenReturn(Optional.of(mockClassification));
        doNothing().when(classificationDamRepository).deleteById(1L);

        // When
        classificationDamService.deleteById(1L);

        // Then
        verify(classificationDamRepository).findById(1L);
        verify(classificationDamRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when deleting non-existent classification")
    void shouldThrowNotFoundExceptionWhenDeletingNonExistent() {
        // Given
        when(classificationDamRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> classificationDamService.deleteById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Classificação de barragem não encontrada para exclusão!");

        verify(classificationDamRepository).findById(1L);
        verify(classificationDamRepository, never()).deleteById(any());
    }

    // ==================== Find Tests ====================
    @Test
    @DisplayName("Should find classification by ID successfully")
    void shouldFindClassificationByIdSuccessfully() {
        // Given
        when(classificationDamRepository.findById(1L)).thenReturn(Optional.of(mockClassification));

        // When
        ClassificationDamEntity result = classificationDamService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getClassification()).isEqualTo("A");
        verify(classificationDamRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when classification not found by ID")
    void shouldThrowNotFoundExceptionWhenClassificationNotFoundById() {
        // Given
        when(classificationDamRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> classificationDamService.findById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Classificação de barragem não encontrada!");

        verify(classificationDamRepository).findById(1L);
    }

    @Test
    @DisplayName("Should find all classifications ordered by ID")
    void shouldFindAllClassificationsOrderedById() {
        // Given
        ClassificationDamEntity classA = new ClassificationDamEntity();
        classA.setId(1L);
        classA.setClassification("A");

        ClassificationDamEntity classB = new ClassificationDamEntity();
        classB.setId(2L);
        classB.setClassification("B");

        ClassificationDamEntity classC = new ClassificationDamEntity();
        classC.setId(3L);
        classC.setClassification("C");

        List<ClassificationDamEntity> classifications = Arrays.asList(classA, classB, classC);
        when(classificationDamRepository.findAllByOrderByIdAsc()).thenReturn(classifications);

        // When
        List<ClassificationDamEntity> result = classificationDamService.findAll();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getClassification()).isEqualTo("A");
        assertThat(result.get(1).getClassification()).isEqualTo("B");
        assertThat(result.get(2).getClassification()).isEqualTo("C");
        verify(classificationDamRepository).findAllByOrderByIdAsc();
    }

    @Test
    @DisplayName("Should return empty list when no classifications exist")
    void shouldReturnEmptyListWhenNoClassificationsExist() {
        // Given
        when(classificationDamRepository.findAllByOrderByIdAsc()).thenReturn(Arrays.asList());

        // When
        List<ClassificationDamEntity> result = classificationDamService.findAll();

        // Then
        assertThat(result).isEmpty();
        verify(classificationDamRepository).findAllByOrderByIdAsc();
    }
}
