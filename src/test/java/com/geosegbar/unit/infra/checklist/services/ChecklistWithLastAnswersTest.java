package com.geosegbar.unit.infra.checklist.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersAndDamDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersDTO;
import com.geosegbar.infra.checklist.dtos.QuestionWithLastAnswerDTO;
import com.geosegbar.infra.checklist.dtos.TemplateQuestionnaireWithAnswersDTO;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist.services.ChecklistService;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;

/**
 * O payload "com últimas respostas" sai de três consultas (estrutura, opções e
 * última resposta de cada ponto), montadas em memória na ordem da estrutura.
 */
@Tag("unit")
@DisplayName("ChecklistService — checklists com últimas respostas")
class ChecklistWithLastAnswersTest extends BaseUnitTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 3, 4, 10, 15, 30);

    @Mock
    private ChecklistRepository checklistRepository;

    @Mock
    private DamRepository damRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private OptionRepository optionRepository;

    @InjectMocks
    private ChecklistService checklistService;

    /**
     * Linha de ChecklistRepository.LAST_ANSWERS_STRUCTURE, na ordem de colunas
     * documentada lá, com os tipos que o driver devolve.
     */
    private static Object[] structure(long checklistId, String checklistName, long damId, String damName,
            Long checklistTemplateId, Integer templateOrder, Long templateId, String templateName,
            Integer questionOrder, Long questionId, String questionText) {
        return new Object[]{
            checklistId, checklistName, Timestamp.valueOf(CREATED_AT),
            damId, damName, "Cidade " + damId, "MG", -19.5, -43.9,
            checklistTemplateId, templateOrder, templateId, templateName,
            questionOrder, questionId, questionText, questionId == null ? null : "CHECKBOX"};
    }

    private static Object[] option(long questionId, long optionId, String label) {
        return new Object[]{questionId, optionId, label, "Valor " + label};
    }

    private static Object[] answer(long damId, long templateId, long questionId, long answerId,
            long optionId, String label) {
        return new Object[]{damId, templateId, questionId, answerId, optionId, label, "Valor " + label};
    }

    @Test
    @DisplayName("barragem: questionários e perguntas na ordem da estrutura, com opções e última resposta")
    @SuppressWarnings("unchecked")
    void damAssemblesStructureOptionsAndLastAnswers() {
        when(checklistRepository.findLastAnswersStructureByDamId(10L)).thenReturn(List.of(
                structure(5L, "Inspeção", 10L, "Alfa", 50L, 0, 100L, "Talude", 0, 1000L, "Trinca?"),
                structure(5L, "Inspeção", 10L, "Alfa", 50L, 0, 100L, "Talude", 1, 1001L, "Erosão?"),
                structure(5L, "Inspeção", 10L, "Alfa", 51L, 1, 101L, "Vertedouro", null, null, null)));
        when(optionRepository.findOptionRowsByQuestionIds(any())).thenReturn(List.of(
                option(1000L, 1L, "NE"), option(1000L, 3L, "AU"), option(1000L, 7L, "NI"),
                option(1001L, 1L, "NE")));
        when(answerRepository.findLastRelevantAnswers(any(), any(), any())).thenReturn(List.of(
                answer(10L, 100L, 1000L, 9000L, 3L, "AU"),
                // combinação que o filtro por listas devolve mas não é ponto do checklist
                answer(10L, 101L, 1000L, 9001L, 1L, "NE")));

        List<ChecklistWithLastAnswersDTO> result = checklistService.findChecklistsWithLastAnswersForDam(10L);

        assertThat(result).hasSize(1);
        ChecklistWithLastAnswersDTO checklist = result.get(0);
        assertThat(checklist.getId()).isEqualTo(5L);
        assertThat(checklist.getName()).isEqualTo("Inspeção");
        assertThat(checklist.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(checklist.getTemplateQuestionnaires())
                .extracting(TemplateQuestionnaireWithAnswersDTO::getId, TemplateQuestionnaireWithAnswersDTO::getName,
                        TemplateQuestionnaireWithAnswersDTO::getOrderIndex)
                .containsExactly(tuple(100L, "Talude", 0), tuple(101L, "Vertedouro", 1));

        List<QuestionWithLastAnswerDTO> questions = checklist.getTemplateQuestionnaires().get(0).getQuestions();
        assertThat(questions)
                .extracting(QuestionWithLastAnswerDTO::getId, QuestionWithLastAnswerDTO::getQuestionText,
                        QuestionWithLastAnswerDTO::getOrderIndex, QuestionWithLastAnswerDTO::getType)
                .containsExactly(tuple(1000L, "Trinca?", 0, TypeQuestionEnum.CHECKBOX),
                        tuple(1001L, "Erosão?", 1, TypeQuestionEnum.CHECKBOX));

        QuestionWithLastAnswerDTO answered = questions.get(0);
        assertThat(answered.getAllOptions()).extracting(o -> o.getLabel()).containsExactly("NE", "AU", "NI");
        assertThat(answered.getAnswerResponseId()).isEqualTo(9000L);
        assertThat(answered.getLastSelectedOption().getId()).isEqualTo(3L);
        assertThat(answered.getLastSelectedOption().getLabel()).isEqualTo("AU");
        assertThat(answered.getLastSelectedOption().getValue()).isEqualTo("Valor AU");

        QuestionWithLastAnswerDTO unanswered = questions.get(1);
        assertThat(unanswered.getAllOptions()).extracting(o -> o.getLabel()).containsExactly("NE");
        assertThat(unanswered.getAnswerResponseId()).isNull();
        assertThat(unanswered.getLastSelectedOption()).isNull();

        assertThat(checklist.getTemplateQuestionnaires().get(1).getQuestions()).isEmpty();

        ArgumentCaptor<Collection<Long>> questionIds = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection<Long>> templateIds = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Collection<Long>> damIds = ArgumentCaptor.forClass(Collection.class);
        verify(answerRepository).findLastRelevantAnswers(questionIds.capture(), templateIds.capture(), damIds.capture());
        assertThat(questionIds.getValue()).containsExactly(1000L, 1001L);
        assertThat(templateIds.getValue()).containsExactly(100L, 101L);
        assertThat(damIds.getValue()).containsExactly(10L);
        verify(damRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("barragem sem checklist devolve lista vazia, sem consultar opções nem respostas")
    void damWithoutChecklistIsEmpty() {
        when(checklistRepository.findLastAnswersStructureByDamId(10L)).thenReturn(List.of());
        when(damRepository.existsById(10L)).thenReturn(true);

        assertThat(checklistService.findChecklistsWithLastAnswersForDam(10L)).isEmpty();
        verify(optionRepository, never()).findOptionRowsByQuestionIds(any());
        verify(answerRepository, never()).findLastRelevantAnswers(any(), any(), any());
    }

    @Test
    @DisplayName("barragem inexistente é 404, como antes")
    void unknownDamIsNotFound() {
        when(checklistRepository.findLastAnswersStructureByDamId(99L)).thenReturn(List.of());
        when(damRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> checklistService.findChecklistsWithLastAnswersForDam(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Barragem não encontrada!");
    }

    @Test
    @DisplayName("cliente: checklists por nome da barragem e do checklist, com os dados da barragem")
    void clientSortsByDamThenChecklistAndCarriesDamInfo() {
        when(checklistRepository.findLastAnswersStructureByClientId(1L)).thenReturn(List.of(
                structure(5L, "B", 20L, "Zeta", null, null, null, null, null, null, null),
                structure(6L, "Rotina", 10L, "Alfa", null, null, null, null, null, null, null),
                structure(7L, "A", 20L, "Zeta", null, null, null, null, null, null, null)));

        List<ChecklistWithLastAnswersAndDamDTO> result = checklistService.findAllChecklistsWithLastAnswersByClientId(1L);

        assertThat(result).extracting(c -> c.getDam().getName(), ChecklistWithLastAnswersAndDamDTO::getName)
                .containsExactly(tuple("Alfa", "Rotina"), tuple("Zeta", "A"), tuple("Zeta", "B"));
        ChecklistWithLastAnswersAndDamDTO.DamInfoDTO dam = result.get(0).getDam();
        assertThat(dam.getId()).isEqualTo(10L);
        assertThat(dam.getCity()).isEqualTo("Cidade 10");
        assertThat(dam.getState()).isEqualTo("MG");
        assertThat(dam.getLatitude()).isEqualTo(-19.5);
        assertThat(dam.getLongitude()).isEqualTo(-43.9);
        assertThat(result).allSatisfy(c -> assertThat(c.getTemplateQuestionnaires()).isEmpty());
        verify(optionRepository, never()).findOptionRowsByQuestionIds(any());
        verify(answerRepository, never()).findLastRelevantAnswers(any(), any(), any());
    }

    @Test
    @DisplayName("cliente: a última resposta é da barragem do checklist, não de outra com o mesmo questionário")
    void clientKeysLastAnswerByDam() {
        when(checklistRepository.findLastAnswersStructureByClientId(1L)).thenReturn(List.of(
                structure(5L, "Inspeção", 10L, "Alfa", 50L, 0, 100L, "Talude", 0, 1000L, "Trinca?"),
                structure(6L, "Inspeção", 20L, "Beta", 60L, 0, 100L, "Talude", 0, 1000L, "Trinca?")));
        when(optionRepository.findOptionRowsByQuestionIds(any())).thenReturn(List.<Object[]>of(option(1000L, 3L, "AU")));
        when(answerRepository.findLastRelevantAnswers(any(), any(), any())).thenReturn(List.<Object[]>of(
                answer(20L, 100L, 1000L, 9100L, 3L, "AU")));

        List<ChecklistWithLastAnswersAndDamDTO> result = checklistService.findAllChecklistsWithLastAnswersByClientId(1L);

        QuestionWithLastAnswerDTO alfa = result.get(0).getTemplateQuestionnaires().get(0).getQuestions().get(0);
        QuestionWithLastAnswerDTO beta = result.get(1).getTemplateQuestionnaires().get(0).getQuestions().get(0);
        assertThat(alfa.getAnswerResponseId()).isNull();
        assertThat(beta.getAnswerResponseId()).isEqualTo(9100L);
        assertThat(alfa.getAllOptions()).extracting(o -> o.getLabel()).containsExactly("AU");
        assertThat(beta.getAllOptions()).extracting(o -> o.getLabel()).containsExactly("AU");
    }
}
