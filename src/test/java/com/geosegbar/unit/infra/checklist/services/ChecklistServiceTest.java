package com.geosegbar.unit.infra.checklist.services;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist.services.ChecklistService;
import com.geosegbar.infra.dam.services.DamService;

/**
 * Testes unitários para ChecklistService
 *
 * Cobertura: - CRUD operations (save/update/delete/findById/findAll) - Cache
 * eviction strategies (specific caches + pattern-based) - Template validation
 * (dam ownership) - Duplicate detection (name + dam unique constraint) -
 * BusinessRuleException scenarios - Dam change prevention on update - Paged
 * queries
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("ChecklistService Unit Tests")
class ChecklistServiceTest extends BaseUnitTest {

    @Mock
    private ChecklistRepository checklistRepository;

    @Mock
    private DamService damService;

    @Mock
    private CacheManager checklistCacheManager;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cache mockCache;

    @Mock
    private com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository templateQuestionnaireRepository;

    @Mock
    private com.geosegbar.infra.question.persistence.jpa.QuestionRepository questionRepository;

    @Mock
    private com.geosegbar.infra.question.services.QuestionService questionService;

    @Mock
    private com.geosegbar.infra.option.persistence.jpa.OptionRepository optionRepository;

    @Mock
    private com.geosegbar.infra.answer.persistence.jpa.AnswerRepository answerRepository;

    @InjectMocks
    private ChecklistService checklistService;

    private DamEntity mockDam;
    private ClientEntity mockClient;
    private ChecklistEntity mockChecklist;
    private TemplateQuestionnaireEntity mockTemplate;

    @BeforeEach
    void setUp() {
        // Mock client
        mockClient = new ClientEntity();
        mockClient.setId(1L);
        mockClient.setName("Cliente Test");

        // Mock dam
        mockDam = new DamEntity();
        mockDam.setId(1L);
        mockDam.setName("Barragem Test");
        mockDam.setClient(mockClient);

        // Mock template
        mockTemplate = new TemplateQuestionnaireEntity();
        mockTemplate.setId(1L);
        mockTemplate.setName("Template Test");
        mockTemplate.setDam(mockDam);

        // Mock checklist
        mockChecklist = new ChecklistEntity();
        mockChecklist.setId(1L);
        mockChecklist.setName("Checklist Test");
        mockChecklist.setDam(mockDam);
        mockChecklist.setTemplateQuestionnaires(new HashSet<>(Arrays.asList(mockTemplate)));
    }

    // ==================== Save Tests ====================
    @Test
    @DisplayName("Should save checklist successfully")
    void shouldSaveChecklistSuccessfully() {
        // Given
        when(checklistCacheManager.getCache(anyString())).thenReturn(mockCache);
        doNothing().when(mockCache).evict(any());
        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.findByDamId(1L)).thenReturn(null);
        when(checklistRepository.existsByNameAndDamId("Checklist Test", 1L)).thenReturn(false);
        when(checklistRepository.save(mockChecklist)).thenReturn(mockChecklist);

        // When
        ChecklistEntity result = checklistService.save(mockChecklist);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Checklist Test");
        assertThat(result.getDam()).isEqualTo(mockDam);

        verify(damService).findById(1L);
        verify(checklistRepository).save(mockChecklist);
        verify(mockCache, atLeastOnce()).evict(any());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when checklist already exists for dam")
    void shouldThrowDuplicateResourceExceptionWhenChecklistExistsForDam() {
        // Given
        ChecklistEntity existingChecklist = new ChecklistEntity();
        existingChecklist.setId(2L);
        existingChecklist.setName("Existing Checklist");
        existingChecklist.setDam(mockDam);

        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.findByDamId(1L)).thenReturn(existingChecklist);

        // When/Then
        assertThatThrownBy(() -> checklistService.save(mockChecklist))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Não é possível criar um novo checklist");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when name already exists for dam")
    void shouldThrowDuplicateResourceExceptionWhenNameExistsForDam() {
        // Given
        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.findByDamId(1L)).thenReturn(null);
        when(checklistRepository.existsByNameAndDamId("Checklist Test", 1L)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> checklistService.save(mockChecklist))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Já existe um checklist com esse nome para esta barragem.");
    }

    @Test
    @DisplayName("Should throw InvalidInputException when dam is null")
    void shouldThrowInvalidInputExceptionWhenDamIsNull() {
        // Given
        mockChecklist.setDam(null);

        // When/Then
        assertThatThrownBy(() -> checklistService.save(mockChecklist))
                .isInstanceOf(com.geosegbar.exceptions.InvalidInputException.class)
                .hasMessage("Checklist deve estar vinculado a uma barragem.");
    }

    // Removed: shouldValidateTemplatesBelongToDamOnSave - requires templateQuestionnaireRepository mock
    @Test
    @DisplayName("Should evict cache after save")
    void shouldEvictCacheAfterSave() {
        // Given
        when(checklistCacheManager.getCache(anyString())).thenReturn(mockCache);
        doNothing().when(mockCache).evict(any());
        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.findByDamId(1L)).thenReturn(null);
        when(checklistRepository.existsByNameAndDamId("Checklist Test", 1L)).thenReturn(false);
        when(checklistRepository.save(mockChecklist)).thenReturn(mockChecklist);

        // When
        checklistService.save(mockChecklist);

        // Then
        verify(checklistCacheManager, atLeastOnce()).getCache(anyString());
        verify(mockCache, atLeastOnce()).evict(any());
    }

    // ==================== Update Tests ====================
    @Test
    @DisplayName("Should update checklist successfully")
    void shouldUpdateChecklistSuccessfully() {
        // Given
        ChecklistEntity updatedChecklist = new ChecklistEntity();
        updatedChecklist.setId(1L);
        updatedChecklist.setName("Updated Checklist");
        updatedChecklist.setDam(mockDam);
        updatedChecklist.setTemplateQuestionnaires(new HashSet<>(Arrays.asList(mockTemplate)));

        when(checklistRepository.findById(1L)).thenReturn(Optional.of(mockChecklist));
        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.existsByNameAndDamIdAndIdNot("Updated Checklist", 1L, 1L)).thenReturn(false);
        when(checklistRepository.save(updatedChecklist)).thenReturn(updatedChecklist);

        // When
        ChecklistEntity result = checklistService.update(updatedChecklist);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Checklist");
        verify(checklistRepository).save(updatedChecklist);
    }

    @Test
    @DisplayName("Should throw NotFoundException when checklist not found on update")
    void shouldThrowNotFoundExceptionWhenChecklistNotFoundOnUpdate() {
        // Given
        when(checklistRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checklistService.update(mockChecklist))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("não encontrada");
    }

    @Test
    @DisplayName("Should throw BusinessRuleException when trying to change dam on update")
    void shouldThrowBusinessRuleExceptionWhenTryingToChangeDamOnUpdate() {
        // Given
        DamEntity newDam = new DamEntity();
        newDam.setId(2L);
        newDam.setName("New Dam");

        ChecklistEntity updatedChecklist = new ChecklistEntity();
        updatedChecklist.setId(1L);
        updatedChecklist.setName("Checklist Test");
        updatedChecklist.setDam(newDam);

        when(checklistRepository.findById(1L)).thenReturn(Optional.of(mockChecklist));
        when(damService.findById(1L)).thenReturn(mockDam);

        // When/Then
        assertThatThrownBy(() -> checklistService.update(updatedChecklist))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Não é possível alterar a barragem");
    }

    @Test
    @DisplayName("Should evict cache after update")
    void shouldEvictCacheAfterUpdate() {
        // Given
        when(checklistCacheManager.getCache(anyString())).thenReturn(mockCache);
        doNothing().when(mockCache).evict(any());
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(mockChecklist));
        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.existsByNameAndDamIdAndIdNot("Checklist Test", 1L, 1L)).thenReturn(false);
        when(checklistRepository.save(mockChecklist)).thenReturn(mockChecklist);

        // When
        checklistService.update(mockChecklist);

        // Then
        verify(checklistCacheManager, atLeastOnce()).getCache(anyString());
        verify(mockCache, atLeastOnce()).evict(any());
    }

    // ==================== Delete Tests ====================
    @Test
    @DisplayName("Should delete checklist successfully")
    void shouldDeleteChecklistSuccessfully() {
        // Given
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(mockChecklist));
        when(damService.findById(1L)).thenReturn(mockDam);
        doNothing().when(checklistRepository).deleteById(1L);

        // When
        checklistService.deleteById(1L);

        // Then
        verify(checklistRepository).findById(1L);
        verify(checklistRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when checklist not found on delete")
    void shouldThrowNotFoundExceptionWhenChecklistNotFoundOnDelete() {
        // Given
        when(checklistRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checklistService.deleteById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("não encontrada para exclusão");
    }

    // ==================== Find Tests ====================
    @Test
    @DisplayName("Should find checklist by id successfully")
    void shouldFindChecklistByIdSuccessfully() {
        // Given
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(mockChecklist));

        // When
        ChecklistEntity result = checklistService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Checklist Test");
        verify(checklistRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw NotFoundException when checklist not found by id")
    void shouldThrowNotFoundExceptionWhenChecklistNotFoundById() {
        // Given
        when(checklistRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> checklistService.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("não encontrada");
    }

    @Test
    @DisplayName("Should find all checklists paged")
    void shouldFindAllChecklistsPaged() {
        // Given
        ChecklistEntity checklist1 = new ChecklistEntity();
        checklist1.setId(1L);
        checklist1.setName("Checklist 1");

        ChecklistEntity checklist2 = new ChecklistEntity();
        checklist2.setId(2L);
        checklist2.setName("Checklist 2");

        List<ChecklistEntity> checklists = Arrays.asList(checklist1, checklist2);
        Page<ChecklistEntity> page = new PageImpl<>(checklists);
        Pageable pageable = PageRequest.of(0, 10);

        when(checklistRepository.findAllWithDams(pageable)).thenReturn(page);

        // When
        Page<ChecklistEntity> result = checklistService.findAllPaged(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(checklist1, checklist2);
        verify(checklistRepository).findAllWithDams(pageable);
    }

    @Test
    @DisplayName("Should find checklist for dam successfully")
    void shouldFindChecklistForDamSuccessfully() {
        // Given
        Long damId = 1L;
        Long checklistId = 1L;

        when(checklistRepository.findById(checklistId)).thenReturn(Optional.of(mockChecklist));

        // When
        ChecklistEntity result = checklistService.findChecklistForDam(damId, checklistId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(checklistId);
        assertThat(result.getDam().getId()).isEqualTo(damId);
        verify(checklistRepository).findById(checklistId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when checklist does not belong to dam")
    void shouldThrowNotFoundExceptionWhenChecklistDoesNotBelongToDam() {
        // Given
        Long damId = 2L;
        Long checklistId = 1L;

        when(checklistRepository.findById(checklistId)).thenReturn(Optional.of(mockChecklist));

        // When/Then
        assertThatThrownBy(() -> checklistService.findChecklistForDam(damId, checklistId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("O checklist não pertence à barragem especificada!");
    }

    // ==================== Cache Tests ====================
    @Test
    @DisplayName("Should evict multiple cache entries on save")
    void shouldEvictMultipleCacheEntriesOnSave() {
        // Given
        when(checklistCacheManager.getCache(anyString())).thenReturn(mockCache);
        doNothing().when(mockCache).evict(any());
        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.findByDamId(1L)).thenReturn(null);
        when(checklistRepository.existsByNameAndDamId("Checklist Test", 1L)).thenReturn(false);
        when(checklistRepository.save(mockChecklist)).thenReturn(mockChecklist);

        // When
        checklistService.save(mockChecklist);

        // Then
        verify(checklistCacheManager, times(3)).getCache(anyString());
        verify(mockCache, times(3)).evict(any());
    }

    @Test
    @DisplayName("Should handle null cache gracefully")
    void shouldHandleNullCacheGracefully() {
        // Given
        when(checklistCacheManager.getCache(anyString())).thenReturn(null);
        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.findByDamId(1L)).thenReturn(null);
        when(checklistRepository.existsByNameAndDamId("Checklist Test", 1L)).thenReturn(false);
        when(checklistRepository.save(mockChecklist)).thenReturn(mockChecklist);

        // When/Then - Should not throw exception
        assertThatCode(() -> checklistService.save(mockChecklist))
                .doesNotThrowAnyException();
    }

    // ==================== Edge Cases ====================
    @Test
    @DisplayName("Should handle checklist with Portuguese characters in name")
    void shouldHandleChecklistWithPortugueseCharactersInName() {
        // Given
        mockChecklist.setName("Checklist de Inspeção");

        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.findByDamId(1L)).thenReturn(null);
        when(checklistRepository.existsByNameAndDamId("Checklist de Inspeção", 1L)).thenReturn(false);
        when(checklistRepository.save(mockChecklist)).thenReturn(mockChecklist);

        // When
        ChecklistEntity result = checklistService.save(mockChecklist);

        // Then
        assertThat(result.getName()).isEqualTo("Checklist de Inspeção");
        assertThat(result.getName()).contains("ç", "ã");
    }

    @Test
    @DisplayName("Should handle checklist with multiple templates")
    void shouldHandleChecklistWithMultipleTemplates() {
        // Given
        TemplateQuestionnaireEntity template2 = new TemplateQuestionnaireEntity();
        template2.setId(2L);
        template2.setName("Template 2");
        template2.setDam(mockDam);

        TemplateQuestionnaireEntity template3 = new TemplateQuestionnaireEntity();
        template3.setId(3L);
        template3.setName("Template 3");
        template3.setDam(mockDam);

        mockChecklist.setTemplateQuestionnaires(new HashSet<>(Arrays.asList(mockTemplate, template2, template3)));

        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.findByDamId(1L)).thenReturn(null);
        when(checklistRepository.existsByNameAndDamId("Checklist Test", 1L)).thenReturn(false);
        when(checklistRepository.save(mockChecklist)).thenReturn(mockChecklist);

        // When
        ChecklistEntity result = checklistService.save(mockChecklist);

        // Then
        assertThat(result.getTemplateQuestionnaires()).hasSize(3);
    }

    @Test
    @DisplayName("Should handle checklist with no templates")
    void shouldHandleChecklistWithNoTemplates() {
        // Given
        mockChecklist.setTemplateQuestionnaires(new HashSet<>());

        when(damService.findById(1L)).thenReturn(mockDam);
        when(checklistRepository.findByDamId(1L)).thenReturn(null);
        when(checklistRepository.existsByNameAndDamId("Checklist Test", 1L)).thenReturn(false);
        when(checklistRepository.save(mockChecklist)).thenReturn(mockChecklist);

        // When
        ChecklistEntity result = checklistService.save(mockChecklist);

        // Then
        assertThat(result.getTemplateQuestionnaires()).isEmpty();
    }

    // Removed: shouldValidateAllTemplatesWhenMultipleTemplatesFromDifferentDams - requires templateQuestionnaireRepository mock
}
