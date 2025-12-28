package com.geosegbar.unit.entities;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class TemplateQuestionnaireEntityTest extends BaseUnitTest {

    private DamEntity dam;

    @BeforeEach
    void setUp() {
        dam = new DamEntity();
        dam.setId(1L);
        dam.setName("Barragem Principal");
    }

    @Test
    @DisplayName("Should create template questionnaire with all required fields")
    void shouldCreateTemplateQuestionnaireWithAllRequiredFields() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setId(1L);
        template.setName("Template de Inspeção Rotineira");
        template.setDam(dam);

        assertThat(template).satisfies(t -> {
            assertThat(t.getId()).isEqualTo(1L);
            assertThat(t.getName()).isEqualTo("Template de Inspeção Rotineira");
            assertThat(t.getDam()).isEqualTo(dam);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity(
                1L,
                "Template Padrão",
                dam,
                null,
                null
        );

        assertThat(template.getId()).isEqualTo(1L);
        assertThat(template.getName()).isEqualTo("Template Padrão");
        assertThat(template.getDam()).isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Dam")
    void shouldMaintainManyToOneRelationshipWithDam() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setDam(dam);

        assertThat(template.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of template questions")
    void shouldMaintainOneToManyCollectionOfTemplateQuestions() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName("Template Principal");

        TemplateQuestionnaireQuestionEntity templateQuestion = new TemplateQuestionnaireQuestionEntity();
        templateQuestion.setId(1L);
        templateQuestion.setTemplateQuestionnaire(template);
        templateQuestion.setOrderIndex(0);

        template.getTemplateQuestions().add(templateQuestion);

        assertThat(template.getTemplateQuestions())
                .isNotNull()
                .hasSize(1)
                .contains(templateQuestion);
    }

    @Test
    @DisplayName("Should initialize empty template questions collection by default")
    void shouldInitializeEmptyTemplateQuestionsCollectionByDefault() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();

        assertThat(template.getTemplateQuestions()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain ManyToMany collection of checklists")
    void shouldMaintainManyToManyCollectionOfChecklists() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName("Template de Segurança");

        ChecklistEntity checklist = new ChecklistEntity();
        checklist.setId(1L);

        template.getChecklists().add(checklist);

        assertThat(template.getChecklists())
                .isNotNull()
                .hasSize(1)
                .contains(checklist);
    }

    @Test
    @DisplayName("Should initialize empty checklists collection by default")
    void shouldInitializeEmptyChecklistsCollectionByDefault() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();

        assertThat(template.getChecklists()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support multiple template questions per template")
    void shouldSupportMultipleTemplateQuestionsPerTemplate() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName("Template Completo");

        TemplateQuestionnaireQuestionEntity question1 = new TemplateQuestionnaireQuestionEntity();
        question1.setId(1L);
        question1.setOrderIndex(0);

        TemplateQuestionnaireQuestionEntity question2 = new TemplateQuestionnaireQuestionEntity();
        question2.setId(2L);
        question2.setOrderIndex(1);

        TemplateQuestionnaireQuestionEntity question3 = new TemplateQuestionnaireQuestionEntity();
        question3.setId(3L);
        question3.setOrderIndex(2);

        template.getTemplateQuestions().add(question1);
        template.getTemplateQuestions().add(question2);
        template.getTemplateQuestions().add(question3);

        assertThat(template.getTemplateQuestions()).hasSize(3);
    }

    @Test
    @DisplayName("Should support multiple templates per dam")
    void shouldSupportMultipleTemplatesPerDam() {

        TemplateQuestionnaireEntity template1 = new TemplateQuestionnaireEntity();
        template1.setId(1L);
        template1.setName("Template Mensal");
        template1.setDam(dam);

        TemplateQuestionnaireEntity template2 = new TemplateQuestionnaireEntity();
        template2.setId(2L);
        template2.setName("Template Trimestral");
        template2.setDam(dam);

        assertThat(template1.getDam()).isEqualTo(template2.getDam());
        assertThat(template1.getName()).isNotEqualTo(template2.getName());
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName("Template de Inspeção e Manutenção");

        assertThat(template.getName()).contains("ç", "ã");
    }

    @Test
    @DisplayName("Should support descriptive template names")
    void shouldSupportDescriptiveTemplateNames() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName("Template Completo de Inspeção de Segurança");

        assertThat(template.getName()).hasSize(42);
    }

    @Test
    @DisplayName("Should support short template names")
    void shouldSupportShortTemplateNames() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName("T1");

        assertThat(template.getName()).hasSize(2);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setId(1L);
        template.setName("Template Inicial");

        Long originalId = template.getId();

        template.setName("Template Atualizado");

        assertThat(template.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support bidirectional relationship with template questions")
    void shouldSupportBidirectionalRelationshipWithTemplateQuestions() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setId(1L);
        template.setName("Template Principal");

        TemplateQuestionnaireQuestionEntity templateQuestion = new TemplateQuestionnaireQuestionEntity();
        templateQuestion.setId(1L);
        templateQuestion.setTemplateQuestionnaire(template);
        templateQuestion.setOrderIndex(0);

        template.getTemplateQuestions().add(templateQuestion);

        assertThat(templateQuestion.getTemplateQuestionnaire()).isEqualTo(template);
        assertThat(template.getTemplateQuestions()).contains(templateQuestion);
    }

    @Test
    @DisplayName("Should support adding and removing template questions")
    void shouldSupportAddingAndRemovingTemplateQuestions() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        TemplateQuestionnaireQuestionEntity templateQuestion = new TemplateQuestionnaireQuestionEntity();
        templateQuestion.setId(1L);

        template.getTemplateQuestions().add(templateQuestion);

        assertThat(template.getTemplateQuestions()).hasSize(1);

        template.getTemplateQuestions().remove(templateQuestion);

        assertThat(template.getTemplateQuestions()).isEmpty();
    }

    @Test
    @DisplayName("Should support multiple checklists per template")
    void shouldSupportMultipleChecklistsPerTemplate() {

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setName("Template Compartilhado");

        ChecklistEntity checklist1 = new ChecklistEntity();
        checklist1.setId(1L);

        ChecklistEntity checklist2 = new ChecklistEntity();
        checklist2.setId(2L);

        template.getChecklists().add(checklist1);
        template.getChecklists().add(checklist2);

        assertThat(template.getChecklists()).hasSize(2);
    }

    @Test
    @DisplayName("Should support common template categories")
    void shouldSupportCommonTemplateCategories() {

        TemplateQuestionnaireEntity routineTemplate = new TemplateQuestionnaireEntity();
        routineTemplate.setName("Inspeção Rotineira");

        TemplateQuestionnaireEntity safetyTemplate = new TemplateQuestionnaireEntity();
        safetyTemplate.setName("Inspeção de Segurança");

        TemplateQuestionnaireEntity emergencyTemplate = new TemplateQuestionnaireEntity();
        emergencyTemplate.setName("Inspeção de Emergência");

        assertThat(routineTemplate.getName()).isNotEqualTo(safetyTemplate.getName());
        assertThat(safetyTemplate.getName()).isNotEqualTo(emergencyTemplate.getName());
    }
}
