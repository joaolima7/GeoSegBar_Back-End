package com.geosegbar.unit.entities;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - ChecklistEntity")
class ChecklistEntityTest extends BaseUnitTest {

    private DamEntity dam;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        dam = TestDataBuilder.dam().withName("Barragem Teste").build();
    }

    @Test
    @DisplayName("Should create checklist with required fields")
    void shouldCreateChecklistWithRequiredFields() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        checklist.setId(1L);
        checklist.setName("Checklist de Segurança");
        checklist.setDam(dam);

        // Then
        assertThat(checklist).satisfies(c -> {
            assertThat(c.getId()).isEqualTo(1L);
            assertThat(c.getName()).isEqualTo("Checklist de Segurança");
            assertThat(c.getDam()).isEqualTo(dam);
            assertThat(c.getTemplateQuestionnaires()).isNotNull().isEmpty();
        });
    }

    @Test
    @DisplayName("Should create checklist using all args constructor")
    void shouldCreateChecklistUsingAllArgsConstructor() {
        // Given & When
        LocalDateTime now = LocalDateTime.now();
        HashSet<TemplateQuestionnaireEntity> templates = new HashSet<>();

        ChecklistEntity checklist = new ChecklistEntity(
                1L,
                "Checklist Mensal",
                now,
                templates,
                dam
        );

        // Then
        assertThat(checklist).satisfies(c -> {
            assertThat(c.getId()).isEqualTo(1L);
            assertThat(c.getName()).isEqualTo("Checklist Mensal");
            assertThat(c.getCreatedAt()).isEqualTo(now);
            assertThat(c.getTemplateQuestionnaires()).isEqualTo(templates);
            assertThat(c.getDam()).isEqualTo(dam);
        });
    }

    @Test
    @DisplayName("Should create checklist with no args constructor")
    void shouldCreateChecklistWithNoArgsConstructor() {
        // Given & When
        ChecklistEntity checklist = new ChecklistEntity();

        // Then
        assertThat(checklist).isNotNull();
        assertThat(checklist.getId()).isNull();
        assertThat(checklist.getName()).isNull();
        assertThat(checklist.getCreatedAt()).isNull();
        assertThat(checklist.getTemplateQuestionnaires()).isNotNull().isEmpty();
        assertThat(checklist.getDam()).isNull();
    }

    @Test
    @DisplayName("Should initialize template questionnaires as empty HashSet")
    void shouldInitializeTemplateQuestionnairesAsEmptyHashSet() {
        // Given & When
        ChecklistEntity checklist = new ChecklistEntity();

        // Then
        assertThat(checklist.getTemplateQuestionnaires())
                .isNotNull()
                .isInstanceOf(HashSet.class)
                .isEmpty();
    }

    @Test
    @DisplayName("Should add template questionnaire to checklist")
    void shouldAddTemplateQuestionnaireToChecklist() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        checklist.setName("Checklist Principal");

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setId(1L);
        template.setName("Template de Inspeção");

        // When
        checklist.getTemplateQuestionnaires().add(template);

        // Then
        assertThat(checklist.getTemplateQuestionnaires())
                .hasSize(1)
                .contains(template);
    }

    @Test
    @DisplayName("Should add multiple template questionnaires")
    void shouldAddMultipleTemplateQuestionnaires() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        checklist.setName("Checklist Completo");

        TemplateQuestionnaireEntity template1 = new TemplateQuestionnaireEntity();
        template1.setId(1L);
        template1.setName("Template Estrutura");

        TemplateQuestionnaireEntity template2 = new TemplateQuestionnaireEntity();
        template2.setId(2L);
        template2.setName("Template Hidráulico");

        TemplateQuestionnaireEntity template3 = new TemplateQuestionnaireEntity();
        template3.setId(3L);
        template3.setName("Template Elétrico");

        // When
        checklist.getTemplateQuestionnaires().add(template1);
        checklist.getTemplateQuestionnaires().add(template2);
        checklist.getTemplateQuestionnaires().add(template3);

        // Then
        assertThat(checklist.getTemplateQuestionnaires())
                .hasSize(3)
                .containsExactlyInAnyOrder(template1, template2, template3);
    }

    @Test
    @DisplayName("Should remove template questionnaire from checklist")
    void shouldRemoveTemplateQuestionnaireFromChecklist() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        TemplateQuestionnaireEntity template1 = new TemplateQuestionnaireEntity();
        template1.setId(1L);
        TemplateQuestionnaireEntity template2 = new TemplateQuestionnaireEntity();
        template2.setId(2L);

        checklist.getTemplateQuestionnaires().add(template1);
        checklist.getTemplateQuestionnaires().add(template2);

        // When
        checklist.getTemplateQuestionnaires().remove(template1);

        // Then
        assertThat(checklist.getTemplateQuestionnaires())
                .hasSize(1)
                .contains(template2)
                .doesNotContain(template1);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with dam")
    void shouldMaintainManyToOneRelationshipWithDam() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        checklist.setName("Checklist Dam Test");
        checklist.setDam(dam);

        // Then
        assertThat(checklist.getDam())
                .isNotNull()
                .isEqualTo(dam);
        assertThat(checklist.getDam().getName()).isEqualTo("Barragem Teste");
    }

    @Test
    @DisplayName("Should handle createdAt timestamp")
    void shouldHandleCreatedAtTimestamp() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        LocalDateTime now = LocalDateTime.now();
        checklist.setCreatedAt(now);

        // Then
        assertThat(checklist.getCreatedAt())
                .isNotNull()
                .isEqualTo(now);
    }

    @Test
    @DisplayName("Should handle different checklist names")
    void shouldHandleDifferentChecklistNames() {
        // Given
        ChecklistEntity daily = new ChecklistEntity();
        daily.setName("Checklist Diário");

        ChecklistEntity weekly = new ChecklistEntity();
        weekly.setName("Checklist Semanal");

        ChecklistEntity monthly = new ChecklistEntity();
        monthly.setName("Checklist Mensal");

        ChecklistEntity annual = new ChecklistEntity();
        annual.setName("Checklist Anual");

        // Then
        assertThat(daily.getName()).isEqualTo("Checklist Diário");
        assertThat(weekly.getName()).isEqualTo("Checklist Semanal");
        assertThat(monthly.getName()).isEqualTo("Checklist Mensal");
        assertThat(annual.getName()).isEqualTo("Checklist Anual");
    }

    @Test
    @DisplayName("Should support checklist name with special characters")
    void shouldSupportChecklistNameWithSpecialCharacters() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        checklist.setName("Checklist - Inspeção de Segurança (2024)");

        // Then
        assertThat(checklist.getName())
                .contains("-")
                .contains("(")
                .contains(")");
    }

    @Test
    @DisplayName("Should support long checklist names")
    void shouldSupportLongChecklistNames() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        String longName = "Checklist Completo de Inspeção Técnica e Segurança Estrutural da Barragem com Análise de Instrumentação";
        checklist.setName(longName);

        // Then
        assertThat(checklist.getName()).isEqualTo(longName);
        assertThat(checklist.getName().length()).isGreaterThan(50);
    }

    @Test
    @DisplayName("Should associate multiple checklists with same dam")
    void shouldAssociateMultipleChecklistsWithSameDam() {
        // Given
        ChecklistEntity checklist1 = new ChecklistEntity();
        checklist1.setName("Checklist 1");
        checklist1.setDam(dam);

        ChecklistEntity checklist2 = new ChecklistEntity();
        checklist2.setName("Checklist 2");
        checklist2.setDam(dam);

        // Then
        assertThat(checklist1.getDam()).isEqualTo(dam);
        assertThat(checklist2.getDam()).isEqualTo(dam);
        assertThat(checklist1.getDam()).isEqualTo(checklist2.getDam());
    }

    @Test
    @DisplayName("Should clear all template questionnaires")
    void shouldClearAllTemplateQuestionnaires() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        TemplateQuestionnaireEntity template1 = new TemplateQuestionnaireEntity();
        template1.setId(1L);
        TemplateQuestionnaireEntity template2 = new TemplateQuestionnaireEntity();
        template2.setId(2L);

        checklist.getTemplateQuestionnaires().add(template1);
        checklist.getTemplateQuestionnaires().add(template2);

        // When
        checklist.getTemplateQuestionnaires().clear();

        // Then
        assertThat(checklist.getTemplateQuestionnaires()).isEmpty();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        checklist.setId(1L);
        checklist.setName("Original Name");
        checklist.setDam(dam);

        Long originalId = checklist.getId();

        // When
        checklist.setName("Updated Name");
        DamEntity newDam = TestDataBuilder.dam().withName("Nova Barragem").build();
        checklist.setDam(newDam);

        // Then
        assertThat(checklist.getId()).isEqualTo(originalId);
        assertThat(checklist.getName()).isEqualTo("Updated Name");
        assertThat(checklist.getDam()).isEqualTo(newDam);
    }

    @Test
    @DisplayName("Should handle checklist with no templates")
    void shouldHandleChecklistWithNoTemplates() {
        // Given
        ChecklistEntity checklist = new ChecklistEntity();
        checklist.setName("Empty Checklist");
        checklist.setDam(dam);

        // Then
        assertThat(checklist.getTemplateQuestionnaires()).isEmpty();
        assertThat(checklist.getName()).isNotBlank();
        assertThat(checklist.getDam()).isNotNull();
    }
}
