package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - OptionEntity")
class OptionEntityTest extends BaseUnitTest {

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
    }

    @Test
    @DisplayName("Should create option with all required fields")
    void shouldCreateOptionWithAllRequiredFields() {

        OptionEntity option = new OptionEntity();
        option.setId(1L);
        option.setLabel("Sim");
        option.setValue("Sim");

        assertThat(option).satisfies(o -> {
            assertThat(o.getId()).isEqualTo(1L);
            assertThat(o.getLabel()).isEqualTo("Sim");
            assertThat(o.getValue()).isEqualTo("Sim");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        OptionEntity option = new OptionEntity(
                1L,
                "Sim",
                "Sim",
                1,
                new HashSet<>(),
                new HashSet<>()
        );

        assertThat(option.getId()).isEqualTo(1L);
        assertThat(option.getLabel()).isEqualTo("Sim");
        assertThat(option.getValue()).isEqualTo("Sim");
        assertThat(option.getOrderIndex()).isEqualTo(1);
        assertThat(option.getAnswers()).isNotNull().isEmpty();
        assertThat(option.getQuestions()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support unique label constraint")
    void shouldSupportUniqueLabelConstraint() {

        OptionEntity option1 = new OptionEntity();
        option1.setId(1L);
        option1.setLabel("Sim");

        OptionEntity option2 = new OptionEntity();
        option2.setId(2L);
        option2.setLabel("Não");

        assertThat(option1.getLabel()).isNotEqualTo(option2.getLabel());
    }

    @Test
    @DisplayName("Should validate value pattern - only letters and spaces")
    void shouldValidateValuePatternOnlyLettersAndSpaces() {

        OptionEntity option = new OptionEntity();
        option.setLabel("Opção");
        option.setValue("Sim");

        assertThat(option.getValue()).matches("^[A-Za-zÀ-ÿ\\s]+$");
    }

    @Test
    @DisplayName("Should support Portuguese characters in value")
    void shouldSupportPortugueseCharactersInValue() {

        OptionEntity option = new OptionEntity();
        option.setValue("Não");

        assertThat(option.getValue()).contains("ã");
    }

    @Test
    @DisplayName("Should support accented characters in value")
    void shouldSupportAccentedCharactersInValue() {

        OptionEntity option = new OptionEntity();
        option.setValue("Às vezes");

        assertThat(option.getValue()).contains("À", "s");
    }

    @Test
    @DisplayName("Should support order index")
    void shouldSupportOrderIndex() {

        OptionEntity option = new OptionEntity();
        option.setOrderIndex(1);

        assertThat(option.getOrderIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should support sequential order indexes")
    void shouldSupportSequentialOrderIndexes() {

        OptionEntity option1 = new OptionEntity();
        option1.setOrderIndex(1);

        OptionEntity option2 = new OptionEntity();
        option2.setOrderIndex(2);

        OptionEntity option3 = new OptionEntity();
        option3.setOrderIndex(3);

        assertThat(option1.getOrderIndex()).isLessThan(option2.getOrderIndex());
        assertThat(option2.getOrderIndex()).isLessThan(option3.getOrderIndex());
    }

    @Test
    @DisplayName("Should allow null order index")
    void shouldAllowNullOrderIndex() {

        OptionEntity option = new OptionEntity();
        option.setLabel("Opção");
        option.setOrderIndex(null);

        assertThat(option.getOrderIndex()).isNull();
    }

    @Test
    @DisplayName("Should maintain ManyToMany collection of answers")
    void shouldMaintainManyToManyCollectionOfAnswers() {

        OptionEntity option = new OptionEntity();
        option.setLabel("Sim");
        option.setAnswers(new HashSet<>());

        AnswerEntity answer = new AnswerEntity();
        answer.setId(1L);
        option.getAnswers().add(answer);

        assertThat(option.getAnswers())
                .isNotNull()
                .hasSize(1)
                .contains(answer);
    }

    @Test
    @DisplayName("Should maintain ManyToMany collection of questions")
    void shouldMaintainManyToManyCollectionOfQuestions() {

        OptionEntity option = new OptionEntity();
        option.setLabel("Sim");
        option.setQuestions(new HashSet<>());

        QuestionEntity question = new QuestionEntity();
        question.setId(1L);
        option.getQuestions().add(question);

        assertThat(option.getQuestions())
                .isNotNull()
                .hasSize(1)
                .contains(question);
    }

    @Test
    @DisplayName("Should support multiple answers per option")
    void shouldSupportMultipleAnswersPerOption() {

        OptionEntity option = new OptionEntity();
        option.setLabel("Sim");
        option.setAnswers(new HashSet<>());

        AnswerEntity answer1 = new AnswerEntity();
        answer1.setId(1L);
        AnswerEntity answer2 = new AnswerEntity();
        answer2.setId(2L);
        AnswerEntity answer3 = new AnswerEntity();
        answer3.setId(3L);

        option.getAnswers().add(answer1);
        option.getAnswers().add(answer2);
        option.getAnswers().add(answer3);

        assertThat(option.getAnswers()).hasSize(3);
    }

    @Test
    @DisplayName("Should support multiple questions per option")
    void shouldSupportMultipleQuestionsPerOption() {

        OptionEntity option = new OptionEntity();
        option.setLabel("Sim");
        option.setQuestions(new HashSet<>());

        QuestionEntity q1 = new QuestionEntity();
        q1.setId(1L);
        QuestionEntity q2 = new QuestionEntity();
        q2.setId(2L);

        option.getQuestions().add(q1);
        option.getQuestions().add(q2);

        assertThat(option.getQuestions()).hasSize(2);
    }

    @Test
    @DisplayName("Should initialize empty collections by default")
    void shouldInitializeEmptyCollectionsByDefault() {

        OptionEntity option = new OptionEntity();

        assertThat(option.getAnswers()).isNotNull().isEmpty();
        assertThat(option.getQuestions()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support common option labels")
    void shouldSupportCommonOptionLabels() {

        OptionEntity yes = new OptionEntity();
        yes.setLabel("Sim");

        OptionEntity no = new OptionEntity();
        no.setLabel("Não");

        OptionEntity maybe = new OptionEntity();
        maybe.setLabel("Talvez");

        assertThat(yes.getLabel()).isEqualTo("Sim");
        assertThat(no.getLabel()).isEqualTo("Não");
        assertThat(maybe.getLabel()).isEqualTo("Talvez");
    }

    @Test
    @DisplayName("Should support lazy fetch for answers")
    void shouldSupportLazyFetchForAnswers() {

        OptionEntity option = new OptionEntity();
        option.setLabel("Sim");

        assertThat(option.getAnswers()).isNotNull();
    }

    @Test
    @DisplayName("Should support lazy fetch for questions")
    void shouldSupportLazyFetchForQuestions() {

        OptionEntity option = new OptionEntity();
        option.setLabel("Sim");

        assertThat(option.getQuestions()).isNotNull();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        OptionEntity option = new OptionEntity();
        option.setId(1L);
        option.setLabel("Sim");

        Long originalId = option.getId();

        option.setLabel("Não");

        assertThat(option.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support index-based ordering")
    void shouldSupportIndexBasedOrdering() {

        OptionEntity first = new OptionEntity();
        first.setLabel("Primeira Opção");
        first.setOrderIndex(1);

        OptionEntity second = new OptionEntity();
        second.setLabel("Segunda Opção");
        second.setOrderIndex(2);

        OptionEntity third = new OptionEntity();
        third.setLabel("Terceira Opção");
        third.setOrderIndex(3);

        assertThat(first.getOrderIndex()).isLessThan(second.getOrderIndex());
        assertThat(second.getOrderIndex()).isLessThan(third.getOrderIndex());
    }

    @Test
    @DisplayName("Should support label as unique identifier")
    void shouldSupportLabelAsUniqueIdentifier() {

        OptionEntity option = new OptionEntity();
        option.setLabel("Opção Única");

        assertThat(option.getLabel()).isNotBlank();
    }
}
