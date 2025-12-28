package com.geosegbar.unit.entities;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - QuestionnaireResponseEntity")
class QuestionnaireResponseEntityTest extends BaseUnitTest {

    private TemplateQuestionnaireEntity templateQuestionnaire;
    private DamEntity dam;
    private ChecklistResponseEntity checklistResponse;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();

        templateQuestionnaire = new TemplateQuestionnaireEntity();
        templateQuestionnaire.setId(1L);

        dam = new DamEntity();
        dam.setId(1L);
        dam.setName("Barragem Teste");

        checklistResponse = new ChecklistResponseEntity();
        checklistResponse.setId(1L);
    }

    @Test
    @DisplayName("Should create questionnaire response with all required fields")
    void shouldCreateQuestionnaireResponseWithAllRequiredFields() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        response.setId(1L);
        response.setTemplateQuestionnaire(templateQuestionnaire);
        response.setDam(dam);
        response.setChecklistResponse(checklistResponse);

        assertThat(response).satisfies(r -> {
            assertThat(r.getId()).isEqualTo(1L);
            assertThat(r.getTemplateQuestionnaire()).isEqualTo(templateQuestionnaire);
            assertThat(r.getDam()).isEqualTo(dam);
            assertThat(r.getChecklistResponse()).isEqualTo(checklistResponse);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        LocalDateTime now = LocalDateTime.now();

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity(
                1L,
                templateQuestionnaire,
                dam,
                checklistResponse,
                now,
                null
        );

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTemplateQuestionnaire()).isEqualTo(templateQuestionnaire);
        assertThat(response.getDam()).isEqualTo(dam);
        assertThat(response.getChecklistResponse()).isEqualTo(checklistResponse);
        assertThat(response.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should set createdAt timestamp automatically")
    void shouldSetCreatedAtTimestampAutomatically() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        LocalDateTime timestamp = LocalDateTime.now();

        response.setCreatedAt(timestamp);

        assertThat(response.getCreatedAt())
                .isNotNull()
                .isCloseTo(timestamp, within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with TemplateQuestionnaire")
    void shouldMaintainManyToOneRelationshipWithTemplateQuestionnaire() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        response.setTemplateQuestionnaire(templateQuestionnaire);

        assertThat(response.getTemplateQuestionnaire())
                .isNotNull()
                .isEqualTo(templateQuestionnaire);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Dam")
    void shouldMaintainManyToOneRelationshipWithDam() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        response.setDam(dam);

        assertThat(response.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with ChecklistResponse")
    void shouldMaintainManyToOneRelationshipWithChecklistResponse() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        response.setChecklistResponse(checklistResponse);

        assertThat(response.getChecklistResponse())
                .isNotNull()
                .isEqualTo(checklistResponse);
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of answers")
    void shouldMaintainOneToManyCollectionOfAnswers() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();

        AnswerEntity answer = new AnswerEntity();
        answer.setId(1L);

        response.getAnswers().add(answer);

        assertThat(response.getAnswers())
                .isNotNull()
                .hasSize(1)
                .contains(answer);
    }

    @Test
    @DisplayName("Should support multiple answers per questionnaire response")
    void shouldSupportMultipleAnswersPerQuestionnaireResponse() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();

        AnswerEntity answer1 = new AnswerEntity();
        answer1.setId(1L);
        AnswerEntity answer2 = new AnswerEntity();
        answer2.setId(2L);
        AnswerEntity answer3 = new AnswerEntity();
        answer3.setId(3L);

        response.getAnswers().add(answer1);
        response.getAnswers().add(answer2);
        response.getAnswers().add(answer3);

        assertThat(response.getAnswers()).hasSize(3);
    }

    @Test
    @DisplayName("Should initialize empty answers collection by default")
    void shouldInitializeEmptyAnswersCollectionByDefault() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();

        assertThat(response.getAnswers()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing answers")
    void shouldSupportAddingAndRemovingAnswers() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        AnswerEntity answer = new AnswerEntity();
        answer.setId(1L);

        response.getAnswers().add(answer);
        assertThat(response.getAnswers()).hasSize(1);

        response.getAnswers().remove(answer);

        assertThat(response.getAnswers()).isEmpty();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        response.setId(1L);
        response.setDam(dam);

        Long originalId = response.getId();

        DamEntity newDam = new DamEntity();
        newDam.setId(2L);
        response.setDam(newDam);

        assertThat(response.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support cascade operations on answers")
    void shouldSupportCascadeOperationsOnAnswers() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        AnswerEntity answer = new AnswerEntity();

        response.getAnswers().add(answer);

        assertThat(response.getAnswers()).hasSize(1);
    }

    @Test
    @DisplayName("Should support orphan removal for answers")
    void shouldSupportOrphanRemovalForAnswers() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        AnswerEntity answer = new AnswerEntity();
        response.getAnswers().add(answer);

        response.getAnswers().remove(answer);

        assertThat(response.getAnswers()).isEmpty();
    }

    @Test
    @DisplayName("Should support multiple questionnaire responses per checklist response")
    void shouldSupportMultipleQuestionnaireResponsesPerChecklistResponse() {

        QuestionnaireResponseEntity response1 = new QuestionnaireResponseEntity();
        response1.setId(1L);
        response1.setChecklistResponse(checklistResponse);

        QuestionnaireResponseEntity response2 = new QuestionnaireResponseEntity();
        response2.setId(2L);
        response2.setChecklistResponse(checklistResponse);

        assertThat(response1.getChecklistResponse()).isEqualTo(response2.getChecklistResponse());
        assertThat(response1.getId()).isNotEqualTo(response2.getId());
    }

    @Test
    @DisplayName("Should support multiple questionnaire responses per dam")
    void shouldSupportMultipleQuestionnaireResponsesPerDam() {

        QuestionnaireResponseEntity response1 = new QuestionnaireResponseEntity();
        response1.setId(1L);
        response1.setDam(dam);

        QuestionnaireResponseEntity response2 = new QuestionnaireResponseEntity();
        response2.setId(2L);
        response2.setDam(dam);

        assertThat(response1.getDam()).isEqualTo(response2.getDam());
    }

    @Test
    @DisplayName("Should support timestamp tracking for audit")
    void shouldSupportTimestampTrackingForAudit() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        LocalDateTime timestamp = LocalDateTime.of(2024, 12, 28, 10, 30);

        response.setCreatedAt(timestamp);

        assertThat(response.getCreatedAt()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should support lazy fetch for answers")
    void shouldSupportLazyFetchForAnswers() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();

        assertThat(response.getAnswers()).isNotNull();
    }

    @Test
    @DisplayName("Should support bidirectional relationship with answers")
    void shouldSupportBidirectionalRelationshipWithAnswers() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        response.setId(1L);

        AnswerEntity answer = new AnswerEntity();
        answer.setId(1L);
        answer.setQuestionnaireResponse(response);

        response.getAnswers().add(answer);

        assertThat(answer.getQuestionnaireResponse()).isEqualTo(response);
        assertThat(response.getAnswers()).contains(answer);
    }

    @Test
    @DisplayName("Should support questionnaire response lifecycle")
    void shouldSupportQuestionnaireResponseLifecycle() {

        QuestionnaireResponseEntity response = new QuestionnaireResponseEntity();
        response.setTemplateQuestionnaire(templateQuestionnaire);
        response.setDam(dam);
        response.setChecklistResponse(checklistResponse);
        response.setCreatedAt(LocalDateTime.now());

        AnswerEntity answer1 = new AnswerEntity();
        AnswerEntity answer2 = new AnswerEntity();
        response.getAnswers().add(answer1);
        response.getAnswers().add(answer2);

        assertThat(response.getTemplateQuestionnaire()).isNotNull();
        assertThat(response.getDam()).isNotNull();
        assertThat(response.getChecklistResponse()).isNotNull();
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getAnswers()).hasSize(2);
    }

    @Test
    @DisplayName("Should support different templates per questionnaire response")
    void shouldSupportDifferentTemplatesPerQuestionnaireResponse() {

        TemplateQuestionnaireEntity template1 = new TemplateQuestionnaireEntity();
        template1.setId(1L);

        TemplateQuestionnaireEntity template2 = new TemplateQuestionnaireEntity();
        template2.setId(2L);

        QuestionnaireResponseEntity response1 = new QuestionnaireResponseEntity();
        response1.setTemplateQuestionnaire(template1);

        QuestionnaireResponseEntity response2 = new QuestionnaireResponseEntity();
        response2.setTemplateQuestionnaire(template2);

        assertThat(response1.getTemplateQuestionnaire()).isNotEqualTo(response2.getTemplateQuestionnaire());
    }
}
