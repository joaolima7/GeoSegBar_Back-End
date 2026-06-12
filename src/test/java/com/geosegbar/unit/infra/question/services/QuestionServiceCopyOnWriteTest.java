package com.geosegbar.unit.infra.question.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.question.services.QuestionService;
import com.geosegbar.infra.template_questionnaire_question.persistence.jpa.TemplateQuestionnaireQuestionRepository;

@Tag("unit")
@DisplayName("Question copy-on-write update tests")
class QuestionServiceCopyOnWriteTest extends BaseUnitTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private TemplateQuestionnaireQuestionRepository templateQuestionnaireQuestionRepository;

    @InjectMocks
    private QuestionService questionService;

    private ClientEntity client;
    private QuestionEntity existing;
    private QuestionEntity incoming;

    @BeforeEach
    void setUp() {
        client = new ClientEntity();
        client.setId(1L);

        OptionEntity opt = new OptionEntity();
        opt.setId(10L);
        opt.setLabel("AU");

        existing = new QuestionEntity();
        existing.setId(100L);
        existing.setQuestionText("Texto antigo");
        existing.setType(TypeQuestionEnum.CHECKBOX);
        existing.setClient(client);
        existing.getOptions().add(opt);

        incoming = new QuestionEntity();
        incoming.setId(100L);
        incoming.setQuestionText("Texto novo");
        incoming.setType(TypeQuestionEnum.CHECKBOX);
        incoming.setClient(client);
        incoming.getOptions().add(opt);

        when(questionRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(clientRepository.existsById(1L)).thenReturn(true);
        when(answerRepository.findByQuestionIdWithDetails(100L)).thenReturn(List.of());
    }

    @Test
    @DisplayName("applyToAll=true edits the question in place (reflects in all)")
    void applyToAllEditsInPlace() {
        when(questionRepository.save(incoming)).thenReturn(incoming);

        QuestionEntity result = questionService.update(incoming, true, 5L);

        assertThat(result.getId()).isEqualTo(100L);
        verify(questionRepository).save(incoming);
        verify(templateQuestionnaireQuestionRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyToAll=false but question not used elsewhere edits in place")
    void applyToFalseNotSharedEditsInPlace() {
        when(templateQuestionnaireQuestionRepository.countByQuestionIdAndTemplateIdNot(100L, 5L))
                .thenReturn(0L);
        when(questionRepository.save(incoming)).thenReturn(incoming);

        QuestionEntity result = questionService.update(incoming, false, 5L);

        assertThat(result.getId()).isEqualTo(100L);
        verify(questionRepository).save(incoming);
        verify(templateQuestionnaireQuestionRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyToAll=false and shared creates a copy and replaces only in origin template")
    void applyToFalseSharedCopyOnWrite() {
        when(templateQuestionnaireQuestionRepository.countByQuestionIdAndTemplateIdNot(100L, 5L))
                .thenReturn(2L);

        TemplateQuestionnaireQuestionEntity association = new TemplateQuestionnaireQuestionEntity();
        association.setId(900L);
        association.setQuestion(existing);
        association.setOrderIndex(3);
        when(templateQuestionnaireQuestionRepository.findByTemplateQuestionnaireIdAndQuestionId(5L, 100L))
                .thenReturn(Optional.of(association));

        QuestionEntity copy = new QuestionEntity();
        copy.setId(200L);
        when(questionRepository.save(any(QuestionEntity.class))).thenReturn(copy);

        QuestionEntity result = questionService.update(incoming, false, 5L);

        assertThat(result.getId()).isEqualTo(200L);
        // a associação do template de origem passou a apontar para a cópia
        assertThat(association.getQuestion().getId()).isEqualTo(200L);
        assertThat(association.getOrderIndex()).isEqualTo(3);
        verify(templateQuestionnaireQuestionRepository).save(association);
    }

    @Test
    @DisplayName("applyToAll=false, shared, missing templateId throws validation error")
    void applyToFalseSharedMissingTemplateIdThrows() {
        when(templateQuestionnaireQuestionRepository.countByQuestionId(100L)).thenReturn(2L);

        assertThatThrownBy(() -> questionService.update(incoming, false, null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("templateId");
    }
}
