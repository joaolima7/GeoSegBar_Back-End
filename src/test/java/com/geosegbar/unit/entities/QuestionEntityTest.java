package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - QuestionEntity")
class QuestionEntityTest extends BaseUnitTest {

    private ClientEntity client;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();

        client = new ClientEntity();
        client.setId(1L);
        client.setName("Cliente Teste");
    }

    @Test
    @DisplayName("Should create question with all required fields")
    void shouldCreateQuestionWithAllRequiredFields() {

        QuestionEntity question = new QuestionEntity();
        question.setId(1L);
        question.setQuestionText("Qual é o status da barragem?");
        question.setType(TypeQuestionEnum.CHECKBOX);
        question.setClient(client);

        assertThat(question).satisfies(q -> {
            assertThat(q.getId()).isEqualTo(1L);
            assertThat(q.getQuestionText()).isEqualTo("Qual é o status da barragem?");
            assertThat(q.getType()).isEqualTo(TypeQuestionEnum.CHECKBOX);
            assertThat(q.getClient()).isEqualTo(client);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        QuestionEntity question = new QuestionEntity(
                1L,
                "A barragem está operacional?",
                TypeQuestionEnum.CHECKBOX,
                client,
                null
        );

        assertThat(question.getId()).isEqualTo(1L);
        assertThat(question.getQuestionText()).isEqualTo("A barragem está operacional?");
        assertThat(question.getType()).isEqualTo(TypeQuestionEnum.CHECKBOX);
        assertThat(question.getClient()).isEqualTo(client);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Client")
    void shouldMaintainManyToOneRelationshipWithClient() {

        QuestionEntity question = new QuestionEntity();
        question.setClient(client);

        assertThat(question.getClient())
                .isNotNull()
                .isEqualTo(client);
    }

    @Test
    @DisplayName("Should support TypeQuestionEnum CHECKBOX")
    void shouldSupportTypeQuestionEnumCheckbox() {

        QuestionEntity question = new QuestionEntity();
        question.setType(TypeQuestionEnum.CHECKBOX);

        assertThat(question.getType()).isEqualTo(TypeQuestionEnum.CHECKBOX);
    }

    @Test
    @DisplayName("Should support TypeQuestionEnum TEXT")
    void shouldSupportTypeQuestionEnumText() {

        QuestionEntity question = new QuestionEntity();
        question.setType(TypeQuestionEnum.TEXT);

        assertThat(question.getType()).isEqualTo(TypeQuestionEnum.TEXT);
    }

    @Test
    @DisplayName("Should maintain ManyToMany collection of options")
    void shouldMaintainManyToManyCollectionOfOptions() {

        QuestionEntity question = new QuestionEntity();
        question.setQuestionText("Escolha uma opção");
        question.setType(TypeQuestionEnum.CHECKBOX);

        OptionEntity option = new OptionEntity();
        option.setId(1L);
        option.setLabel("Sim");

        question.getOptions().add(option);

        assertThat(question.getOptions())
                .isNotNull()
                .hasSize(1)
                .contains(option);
    }

    @Test
    @DisplayName("Should support multiple options per question")
    void shouldSupportMultipleOptionsPerQuestion() {

        QuestionEntity question = new QuestionEntity();
        question.setType(TypeQuestionEnum.CHECKBOX);

        OptionEntity option1 = new OptionEntity();
        option1.setId(1L);
        option1.setLabel("Sim");

        OptionEntity option2 = new OptionEntity();
        option2.setId(2L);
        option2.setLabel("Não");

        OptionEntity option3 = new OptionEntity();
        option3.setId(3L);
        option3.setLabel("Talvez");

        question.getOptions().add(option1);
        question.getOptions().add(option2);
        question.getOptions().add(option3);

        assertThat(question.getOptions()).hasSize(3);
    }

    @Test
    @DisplayName("Should initialize empty options collection by default")
    void shouldInitializeEmptyOptionsCollectionByDefault() {

        QuestionEntity question = new QuestionEntity();

        assertThat(question.getOptions()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support eager fetch for options")
    void shouldSupportEagerFetchForOptions() {

        QuestionEntity question = new QuestionEntity();
        question.setQuestionText("Pergunta com opções");

        assertThat(question.getOptions()).isNotNull();
    }

    @Test
    @DisplayName("Should support Portuguese characters in question text")
    void shouldSupportPortugueseCharactersInQuestionText() {

        QuestionEntity question = new QuestionEntity();
        question.setQuestionText("Qual é a situação atual da barragem?");

        assertThat(question.getQuestionText()).contains("é", "ã", "ç");
    }

    @Test
    @DisplayName("Should support long question text")
    void shouldSupportLongQuestionText() {

        String longQuestion = "Considerando todos os aspectos técnicos, operacionais e de segurança, "
                + "qual é a sua avaliação sobre o estado atual da estrutura da barragem "
                + "em relação aos padrões estabelecidos pela legislação vigente?";
        QuestionEntity question = new QuestionEntity();
        question.setQuestionText(longQuestion);

        assertThat(question.getQuestionText()).hasSize(200);
    }

    @Test
    @DisplayName("Should support short question text")
    void shouldSupportShortQuestionText() {

        QuestionEntity question = new QuestionEntity();
        question.setQuestionText("OK?");

        assertThat(question.getQuestionText()).hasSize(3);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        QuestionEntity question = new QuestionEntity();
        question.setId(1L);
        question.setQuestionText("Pergunta original");

        Long originalId = question.getId();

        question.setQuestionText("Pergunta atualizada");

        assertThat(question.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support different question types for same client")
    void shouldSupportDifferentQuestionTypesForSameClient() {

        QuestionEntity checkbox = new QuestionEntity();
        checkbox.setId(1L);
        checkbox.setQuestionText("Escolha uma opção");
        checkbox.setType(TypeQuestionEnum.CHECKBOX);
        checkbox.setClient(client);

        QuestionEntity textQuestion = new QuestionEntity();
        textQuestion.setId(2L);
        textQuestion.setQuestionText("Confirma?");
        textQuestion.setType(TypeQuestionEnum.TEXT);
        textQuestion.setClient(client);

        QuestionEntity text = new QuestionEntity();
        text.setId(3L);
        text.setQuestionText("Descreva");
        text.setType(TypeQuestionEnum.TEXT);
        text.setClient(client);

        assertThat(checkbox.getClient()).isEqualTo(textQuestion.getClient());
        assertThat(textQuestion.getClient()).isEqualTo(text.getClient());
        assertThat(checkbox.getType()).isNotEqualTo(textQuestion.getType());
        assertThat(textQuestion.getType()).isEqualTo(text.getType());
    }

    @Test
    @DisplayName("Should support adding and removing options")
    void shouldSupportAddingAndRemovingOptions() {

        QuestionEntity question = new QuestionEntity();
        OptionEntity option = new OptionEntity();
        option.setId(1L);
        option.setLabel("Opção");

        question.getOptions().add(option);
        assertThat(question.getOptions()).hasSize(1);

        question.getOptions().remove(option);

        assertThat(question.getOptions()).isEmpty();
    }

    @Test
    @DisplayName("Should support question with no options for TEXT type")
    void shouldSupportQuestionWithNoOptionsForTextType() {

        QuestionEntity question = new QuestionEntity();
        question.setQuestionText("Descreva o problema");
        question.setType(TypeQuestionEnum.TEXT);

        assertThat(question.getOptions()).isEmpty();
        assertThat(question.getType()).isEqualTo(TypeQuestionEnum.TEXT);
    }

    @Test
    @DisplayName("Should support common safety inspection questions")
    void shouldSupportCommonSafetyInspectionQuestions() {

        QuestionEntity q1 = new QuestionEntity();
        q1.setQuestionText("A barragem apresenta sinais de infiltração?");
        q1.setType(TypeQuestionEnum.CHECKBOX);

        QuestionEntity q2 = new QuestionEntity();
        q2.setQuestionText("Qual o nível de risco identificado?");
        q2.setType(TypeQuestionEnum.CHECKBOX);

        QuestionEntity q3 = new QuestionEntity();
        q3.setQuestionText("Descreva as condições observadas");
        q3.setType(TypeQuestionEnum.TEXT);

        assertThat(q1.getType()).isEqualTo(TypeQuestionEnum.CHECKBOX);
        assertThat(q2.getType()).isEqualTo(TypeQuestionEnum.CHECKBOX);
        assertThat(q3.getType()).isEqualTo(TypeQuestionEnum.TEXT);
    }

    @Test
    @DisplayName("Should support bidirectional relationship with options")
    void shouldSupportBidirectionalRelationshipWithOptions() {

        QuestionEntity question = new QuestionEntity();
        question.setId(1L);
        question.setQuestionText("Pergunta");

        OptionEntity option = new OptionEntity();
        option.setId(1L);
        option.setLabel("Opção");

        question.getOptions().add(option);
        option.getQuestions().add(question);

        assertThat(question.getOptions()).contains(option);
        assertThat(option.getQuestions()).contains(question);
    }

    @Test
    @DisplayName("Should support multiple questions per client")
    void shouldSupportMultipleQuestionsPerClient() {

        QuestionEntity q1 = new QuestionEntity();
        q1.setId(1L);
        q1.setClient(client);

        QuestionEntity q2 = new QuestionEntity();
        q2.setId(2L);
        q2.setClient(client);

        QuestionEntity q3 = new QuestionEntity();
        q3.setId(3L);
        q3.setClient(client);

        assertThat(q1.getClient()).isEqualTo(q2.getClient());
        assertThat(q2.getClient()).isEqualTo(q3.getClient());
    }
}
