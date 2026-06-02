package com.geosegbar.unit.infra.template_questionnaire_question.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionAssociationsRequestDTO;
import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionAssociationsResponseDTO;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;
import com.geosegbar.infra.template_questionnaire_question.dtos.QuestionOrderDTO;
import com.geosegbar.infra.template_questionnaire_question.dtos.QuestionReorderDTO;
import com.geosegbar.infra.template_questionnaire_question.persistence.jpa.TemplateQuestionnaireQuestionRepository;
import com.geosegbar.infra.template_questionnaire_question.services.TemplateQuestionnaireQuestionService;

@Tag("unit")
@DisplayName("Template questionnaire question batch service tests")
class TemplateQuestionnaireQuestionBatchServiceTest extends BaseUnitTest {

    @Mock
    private TemplateQuestionnaireQuestionRepository tqQuestionRepository;

    @Mock
    private TemplateQuestionnaireRepository templateQuestionnaireRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionnaireResponseRepository questionnaireResponseRepository;

    @InjectMocks
    private TemplateQuestionnaireQuestionService service;

    private ClientEntity client;
    private TemplateQuestionnaireEntity templateOne;
    private TemplateQuestionnaireEntity templateTwo;
    private QuestionEntity questionOne;
    private QuestionEntity questionTwo;
    private QuestionEntity questionThree;
    private TemplateQuestionnaireQuestionEntity templateOneQuestionOne;
    private TemplateQuestionnaireQuestionEntity templateOneQuestionTwo;
    private TemplateQuestionnaireQuestionEntity templateTwoQuestionOne;

    @BeforeEach
    void setUp() {
        client = new ClientEntity();
        client.setId(1L);

        DamEntity dam = new DamEntity();
        dam.setId(10L);
        dam.setClient(client);

        templateOne = template(100L, dam);
        templateTwo = template(200L, dam);

        questionOne = question(1L);
        questionTwo = question(2L);
        questionThree = question(3L);

        templateOneQuestionOne = association(1001L, templateOne, questionOne, 1);
        templateOneQuestionTwo = association(1002L, templateOne, questionTwo, 2);
        templateTwoQuestionOne = association(2001L, templateTwo, questionOne, 9);
    }

    @Test
    @DisplayName("Should reorder questions only inside the requested template")
    void shouldReorderQuestionsOnlyInsideRequestedTemplate() {
        QuestionReorderDTO request = new QuestionReorderDTO(List.of(
                new QuestionOrderDTO(2L, 1),
                new QuestionOrderDTO(1L, 2)
        ));

        when(templateQuestionnaireRepository.findById(100L)).thenReturn(Optional.of(templateOne));
        when(tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(100L))
                .thenReturn(List.of(templateOneQuestionOne, templateOneQuestionTwo));
        when(tqQuestionRepository.saveAll(any())).thenAnswer(invocation -> toList(invocation.getArgument(0)));

        TemplateQuestionAssociationsResponseDTO response = service.reorderQuestions(100L, request);

        assertThat(response.getItems())
                .extracting("questionId", "orderIndex")
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(2L, 1),
                        org.assertj.core.api.Assertions.tuple(1L, 2)
                );
        assertThat(templateOneQuestionOne.getOrderIndex()).isEqualTo(2);
        assertThat(templateOneQuestionTwo.getOrderIndex()).isEqualTo(1);
        assertThat(templateTwoQuestionOne.getOrderIndex()).isEqualTo(9);
    }

    @Test
    @DisplayName("Should associate and disassociate questions in one request and persist final order")
    void shouldUpdateQuestionAssociationsInBatch() {
        TemplateQuestionAssociationsRequestDTO request = new TemplateQuestionAssociationsRequestDTO(
                List.of(3L),
                List.of(2L),
                List.of(
                        new QuestionOrderDTO(1L, 1),
                        new QuestionOrderDTO(3L, 2)
                )
        );

        when(templateQuestionnaireRepository.findById(100L)).thenReturn(Optional.of(templateOne));
        when(tqQuestionRepository.findByTemplateQuestionnaireIdOrderByOrderIndex(100L))
                .thenReturn(List.of(templateOneQuestionOne, templateOneQuestionTwo));
        when(questionRepository.findAllById(any())).thenReturn(List.of(questionThree));
        when(tqQuestionRepository.save(any(TemplateQuestionnaireQuestionEntity.class))).thenAnswer(invocation -> {
            TemplateQuestionnaireQuestionEntity saved = invocation.getArgument(0);
            saved.setId(1003L);
            return saved;
        });
        when(tqQuestionRepository.saveAll(any())).thenAnswer(invocation -> toList(invocation.getArgument(0)));

        TemplateQuestionAssociationsResponseDTO response = service.updateQuestionAssociations(100L, request);

        assertThat(response.getAssociatedQuestionIds()).containsExactly(3L);
        assertThat(response.getDisassociatedQuestionIds()).containsExactly(2L);
        assertThat(response.getQuestionCount()).isEqualTo(2);
        assertThat(response.getItems())
                .extracting("questionId", "orderIndex")
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(1L, 1),
                        org.assertj.core.api.Assertions.tuple(3L, 2)
                );

        verify(tqQuestionRepository).delete(templateOneQuestionTwo);
    }

    private TemplateQuestionnaireEntity template(Long id, DamEntity dam) {
        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setId(id);
        template.setDam(dam);
        return template;
    }

    private QuestionEntity question(Long id) {
        QuestionEntity question = new QuestionEntity();
        question.setId(id);
        question.setClient(client);
        return question;
    }

    private TemplateQuestionnaireQuestionEntity association(
            Long id,
            TemplateQuestionnaireEntity template,
            QuestionEntity question,
            Integer orderIndex) {
        TemplateQuestionnaireQuestionEntity association = new TemplateQuestionnaireQuestionEntity();
        association.setId(id);
        association.setTemplateQuestionnaire(template);
        association.setQuestion(question);
        association.setOrderIndex(orderIndex);
        return association;
    }

    private List<TemplateQuestionnaireQuestionEntity> toList(Iterable<TemplateQuestionnaireQuestionEntity> iterable) {
        List<TemplateQuestionnaireQuestionEntity> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}
