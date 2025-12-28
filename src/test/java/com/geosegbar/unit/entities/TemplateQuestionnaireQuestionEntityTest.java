package com.geosegbar.unit.entities;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class TemplateQuestionnaireQuestionEntityTest extends BaseUnitTest {

    private TemplateQuestionnaireEntity templateQuestionnaire;
    private QuestionEntity question;

    @BeforeEach
    void setUp() {
        templateQuestionnaire = new TemplateQuestionnaireEntity();
        templateQuestionnaire.setId(1L);
        templateQuestionnaire.setName("Template de Inspeção");

        question = new QuestionEntity();
        question.setId(1L);
        question.setQuestionText("Há sinais de infiltração?");
    }

    @Test
    @DisplayName("Should create template questionnaire question with all required fields")
    void shouldCreateTemplateQuestionnaireQuestionWithAllRequiredFields() {

        TemplateQuestionnaireQuestionEntity tqq = new TemplateQuestionnaireQuestionEntity();
        tqq.setId(1L);
        tqq.setTemplateQuestionnaire(templateQuestionnaire);
        tqq.setQuestion(question);
        tqq.setOrderIndex(0);

        assertThat(tqq).satisfies(t -> {
            assertThat(t.getId()).isEqualTo(1L);
            assertThat(t.getTemplateQuestionnaire()).isEqualTo(templateQuestionnaire);
            assertThat(t.getQuestion()).isEqualTo(question);
            assertThat(t.getOrderIndex()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        TemplateQuestionnaireQuestionEntity tqq = new TemplateQuestionnaireQuestionEntity(
                1L,
                templateQuestionnaire,
                question,
                0
        );

        assertThat(tqq.getId()).isEqualTo(1L);
        assertThat(tqq.getTemplateQuestionnaire()).isEqualTo(templateQuestionnaire);
        assertThat(tqq.getQuestion()).isEqualTo(question);
        assertThat(tqq.getOrderIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with TemplateQuestionnaire")
    void shouldMaintainManyToOneRelationshipWithTemplateQuestionnaire() {

        TemplateQuestionnaireQuestionEntity tqq = new TemplateQuestionnaireQuestionEntity();
        tqq.setTemplateQuestionnaire(templateQuestionnaire);

        assertThat(tqq.getTemplateQuestionnaire())
                .isNotNull()
                .isEqualTo(templateQuestionnaire);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Question")
    void shouldMaintainManyToOneRelationshipWithQuestion() {

        TemplateQuestionnaireQuestionEntity tqq = new TemplateQuestionnaireQuestionEntity();
        tqq.setQuestion(question);

        assertThat(tqq.getQuestion())
                .isNotNull()
                .isEqualTo(question);
    }

    @Test
    @DisplayName("Should support sequential order indexes")
    void shouldSupportSequentialOrderIndexes() {

        TemplateQuestionnaireQuestionEntity tqq1 = new TemplateQuestionnaireQuestionEntity();
        tqq1.setOrderIndex(0);

        TemplateQuestionnaireQuestionEntity tqq2 = new TemplateQuestionnaireQuestionEntity();
        tqq2.setOrderIndex(1);

        TemplateQuestionnaireQuestionEntity tqq3 = new TemplateQuestionnaireQuestionEntity();
        tqq3.setOrderIndex(2);

        assertThat(tqq1.getOrderIndex()).isEqualTo(0);
        assertThat(tqq2.getOrderIndex()).isEqualTo(1);
        assertThat(tqq3.getOrderIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should support zero-based ordering")
    void shouldSupportZeroBasedOrdering() {

        TemplateQuestionnaireQuestionEntity tqq = new TemplateQuestionnaireQuestionEntity();
        tqq.setOrderIndex(0);

        assertThat(tqq.getOrderIndex()).isZero();
    }

    @Test
    @DisplayName("Should support non-sequential order indexes")
    void shouldSupportNonSequentialOrderIndexes() {

        TemplateQuestionnaireQuestionEntity tqq1 = new TemplateQuestionnaireQuestionEntity();
        tqq1.setOrderIndex(0);

        TemplateQuestionnaireQuestionEntity tqq2 = new TemplateQuestionnaireQuestionEntity();
        tqq2.setOrderIndex(5);

        TemplateQuestionnaireQuestionEntity tqq3 = new TemplateQuestionnaireQuestionEntity();
        tqq3.setOrderIndex(10);

        assertThat(tqq1.getOrderIndex()).isLessThan(tqq2.getOrderIndex());
        assertThat(tqq2.getOrderIndex()).isLessThan(tqq3.getOrderIndex());
    }

    @Test
    @DisplayName("Should support reordering questions")
    void shouldSupportReorderingQuestions() {

        TemplateQuestionnaireQuestionEntity tqq = new TemplateQuestionnaireQuestionEntity();
        tqq.setOrderIndex(0);

        tqq.setOrderIndex(5);

        assertThat(tqq.getOrderIndex()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should support multiple questions per template")
    void shouldSupportMultipleQuestionsPerTemplate() {

        TemplateQuestionnaireQuestionEntity tqq1 = new TemplateQuestionnaireQuestionEntity();
        tqq1.setTemplateQuestionnaire(templateQuestionnaire);
        tqq1.setOrderIndex(0);

        TemplateQuestionnaireQuestionEntity tqq2 = new TemplateQuestionnaireQuestionEntity();
        tqq2.setTemplateQuestionnaire(templateQuestionnaire);
        tqq2.setOrderIndex(1);

        TemplateQuestionnaireQuestionEntity tqq3 = new TemplateQuestionnaireQuestionEntity();
        tqq3.setTemplateQuestionnaire(templateQuestionnaire);
        tqq3.setOrderIndex(2);

        assertThat(tqq1.getTemplateQuestionnaire()).isEqualTo(tqq2.getTemplateQuestionnaire());
        assertThat(tqq2.getTemplateQuestionnaire()).isEqualTo(tqq3.getTemplateQuestionnaire());
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        TemplateQuestionnaireQuestionEntity tqq = new TemplateQuestionnaireQuestionEntity();
        tqq.setId(1L);
        tqq.setOrderIndex(0);

        Long originalId = tqq.getId();

        tqq.setOrderIndex(3);

        assertThat(tqq.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support bidirectional relationship with template")
    void shouldSupportBidirectionalRelationshipWithTemplate() {

        TemplateQuestionnaireQuestionEntity tqq = new TemplateQuestionnaireQuestionEntity();
        tqq.setId(1L);
        tqq.setTemplateQuestionnaire(templateQuestionnaire);
        tqq.setOrderIndex(0);

        templateQuestionnaire.getTemplateQuestions().add(tqq);

        assertThat(tqq.getTemplateQuestionnaire()).isEqualTo(templateQuestionnaire);
        assertThat(templateQuestionnaire.getTemplateQuestions()).contains(tqq);
    }

    @Test
    @DisplayName("Should support large order index values")
    void shouldSupportLargeOrderIndexValues() {

        TemplateQuestionnaireQuestionEntity tqq = new TemplateQuestionnaireQuestionEntity();
        tqq.setOrderIndex(999);

        assertThat(tqq.getOrderIndex()).isEqualTo(999);
    }

    @Test
    @DisplayName("Should support same question in different templates with different orders")
    void shouldSupportSameQuestionInDifferentTemplatesWithDifferentOrders() {

        TemplateQuestionnaireEntity template2 = new TemplateQuestionnaireEntity();
        template2.setId(2L);
        template2.setName("Template 2");

        TemplateQuestionnaireQuestionEntity tqq1 = new TemplateQuestionnaireQuestionEntity();
        tqq1.setTemplateQuestionnaire(templateQuestionnaire);
        tqq1.setQuestion(question);
        tqq1.setOrderIndex(0);

        TemplateQuestionnaireQuestionEntity tqq2 = new TemplateQuestionnaireQuestionEntity();
        tqq2.setTemplateQuestionnaire(template2);
        tqq2.setQuestion(question);
        tqq2.setOrderIndex(5);

        assertThat(tqq1.getQuestion()).isEqualTo(tqq2.getQuestion());
        assertThat(tqq1.getOrderIndex()).isNotEqualTo(tqq2.getOrderIndex());
        assertThat(tqq1.getTemplateQuestionnaire()).isNotEqualTo(tqq2.getTemplateQuestionnaire());
    }

    @Test
    @DisplayName("Should support different questions with same order in different templates")
    void shouldSupportDifferentQuestionsWithSameOrderInDifferentTemplates() {

        TemplateQuestionnaireEntity template2 = new TemplateQuestionnaireEntity();
        template2.setId(2L);

        QuestionEntity question2 = new QuestionEntity();
        question2.setId(2L);

        TemplateQuestionnaireQuestionEntity tqq1 = new TemplateQuestionnaireQuestionEntity();
        tqq1.setTemplateQuestionnaire(templateQuestionnaire);
        tqq1.setQuestion(question);
        tqq1.setOrderIndex(0);

        TemplateQuestionnaireQuestionEntity tqq2 = new TemplateQuestionnaireQuestionEntity();
        tqq2.setTemplateQuestionnaire(template2);
        tqq2.setQuestion(question2);
        tqq2.setOrderIndex(0);

        assertThat(tqq1.getOrderIndex()).isEqualTo(tqq2.getOrderIndex());
        assertThat(tqq1.getQuestion()).isNotEqualTo(tqq2.getQuestion());
        assertThat(tqq1.getTemplateQuestionnaire()).isNotEqualTo(tqq2.getTemplateQuestionnaire());
    }

    @Test
    @DisplayName("Should support ordering for questionnaire flow")
    void shouldSupportOrderingForQuestionnaireFlow() {

        TemplateQuestionnaireQuestionEntity first = new TemplateQuestionnaireQuestionEntity();
        first.setOrderIndex(0);

        TemplateQuestionnaireQuestionEntity middle = new TemplateQuestionnaireQuestionEntity();
        middle.setOrderIndex(1);

        TemplateQuestionnaireQuestionEntity last = new TemplateQuestionnaireQuestionEntity();
        last.setOrderIndex(2);

        assertThat(first.getOrderIndex()).isLessThan(middle.getOrderIndex());
        assertThat(middle.getOrderIndex()).isLessThan(last.getOrderIndex());
    }
}
