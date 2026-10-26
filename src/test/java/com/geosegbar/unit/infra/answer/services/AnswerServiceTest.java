package com.geosegbar.unit.infra.answer.services;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.answer.services.AnswerService;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @InjectMocks
    private AnswerService answerService;

    private AnswerEntity mockAnswer;
    private QuestionEntity mockQuestion;

    @BeforeEach
    void setUp() {
        mockQuestion = new QuestionEntity();
        mockQuestion.setId(1L);
        mockQuestion.setType(TypeQuestionEnum.TEXT);
        mockQuestion.setQuestionText("Test Question");

        mockAnswer = new AnswerEntity();
        mockAnswer.setId(1L);
        mockAnswer.setQuestion(mockQuestion);
        mockAnswer.setComment("Test answer comment");
        mockAnswer.setSelectedOptions(new HashSet<>());
    }

    @Test
    void shouldDeleteAnswerByIdSuccessfully() {
        when(answerRepository.existsById(1L)).thenReturn(true);

        answerService.deleteById(1L);

        verify(answerRepository).existsById(1L);
        verify(answerRepository).deleteById(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDeletingNonExistentAnswer() {
        when(answerRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> answerService.deleteById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Resposta não encontrada para exclusão!");

        verify(answerRepository, never()).deleteById(999L);
    }

    @Test
    void shouldSaveTextAnswerSuccessfully() {
        mockQuestion.setType(TypeQuestionEnum.TEXT);
        mockAnswer.setComment("Valid text answer");
        mockAnswer.setSelectedOptions(new HashSet<>());

        when(answerRepository.save(any(AnswerEntity.class))).thenReturn(mockAnswer);
        when(answerRepository.findByIdWithAllDetails(1L)).thenReturn(Optional.of(mockAnswer));

        AnswerEntity result = answerService.save(mockAnswer);

        assertThat(result).isNotNull();
        assertThat(result.getComment()).isEqualTo("Valid text answer");
        verify(answerRepository).save(mockAnswer);
        verify(answerRepository).findByIdWithAllDetails(1L);
    }

    @Test
    void shouldThrowInvalidInputExceptionWhenTextAnswerHasNoComment() {
        mockQuestion.setType(TypeQuestionEnum.TEXT);
        mockAnswer.setComment(null);

        assertThatThrownBy(() -> answerService.save(mockAnswer))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Respostas para perguntas do tipo TEXTO devem ter o campo de texto preenchido!");
    }

    @Test
    void shouldThrowInvalidInputExceptionWhenTextAnswerHasEmptyComment() {
        mockQuestion.setType(TypeQuestionEnum.TEXT);
        mockAnswer.setComment("   ");

        assertThatThrownBy(() -> answerService.save(mockAnswer))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Respostas para perguntas do tipo TEXTO devem ter o campo de texto preenchido!");
    }

    @Test
    void shouldThrowInvalidInputExceptionWhenTextAnswerHasSelectedOptions() {
        mockQuestion.setType(TypeQuestionEnum.TEXT);
        mockAnswer.setComment("Valid text");

        OptionEntity option = new OptionEntity();
        option.setId(1L);
        mockAnswer.setSelectedOptions(Set.of(option));

        assertThatThrownBy(() -> answerService.save(mockAnswer))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Respostas para perguntas do tipo TEXTO não devem ter opções selecionadas!");
    }

    @Test
    void shouldSaveCheckboxAnswerSuccessfully() {
        mockQuestion.setType(TypeQuestionEnum.CHECKBOX);
        mockAnswer.setComment(null);

        OptionEntity option = new OptionEntity();
        option.setId(1L);
        mockAnswer.setSelectedOptions(Set.of(option));

        when(answerRepository.save(any(AnswerEntity.class))).thenReturn(mockAnswer);
        when(answerRepository.findByIdWithAllDetails(1L)).thenReturn(Optional.of(mockAnswer));

        AnswerEntity result = answerService.save(mockAnswer);

        assertThat(result).isNotNull();
        assertThat(result.getSelectedOptions()).hasSize(1);
        verify(answerRepository).save(mockAnswer);
        verify(answerRepository).findByIdWithAllDetails(1L);
    }

    @Test
    void shouldThrowInvalidInputExceptionWhenCheckboxAnswerHasNoOptions() {
        mockQuestion.setType(TypeQuestionEnum.CHECKBOX);
        mockAnswer.setSelectedOptions(new HashSet<>());

        assertThatThrownBy(() -> answerService.save(mockAnswer))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Respostas para perguntas do tipo CHECKBOX devem ter pelo menos uma opção selecionada!");
    }

    @Test
    void shouldThrowInvalidInputExceptionWhenCheckboxAnswerHasNullOptions() {
        mockQuestion.setType(TypeQuestionEnum.CHECKBOX);
        mockAnswer.setSelectedOptions(null);

        assertThatThrownBy(() -> answerService.save(mockAnswer))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Respostas para perguntas do tipo CHECKBOX devem ter pelo menos uma opção selecionada!");
    }

    @Test
    void shouldThrowInvalidInputExceptionWhenAnswerHasNoQuestion() {
        mockAnswer.setQuestion(null);

        assertThatThrownBy(() -> answerService.save(mockAnswer))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("A resposta deve estar associada a uma pergunta");
    }

    @Test
    void shouldUpdateAnswerSuccessfully() {
        // O update agora carrega a entidade existente, copia comment/selectedOptions
        // para ela, salva a existente e devolve a releitura com todos os detalhes.
        AnswerEntity existing = new AnswerEntity();
        existing.setId(1L);
        existing.setQuestion(mockQuestion);
        existing.setComment("Old comment");
        existing.setSelectedOptions(new HashSet<>());

        mockAnswer.setComment("Updated comment");

        when(answerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(answerRepository.save(existing)).thenReturn(existing);
        when(answerRepository.findByIdWithAllDetails(1L)).thenReturn(Optional.of(existing));

        AnswerEntity result = answerService.update(mockAnswer);

        assertThat(result).isNotNull();
        assertThat(result.getComment()).isEqualTo("Updated comment");
        verify(answerRepository).save(existing);
        verify(answerRepository).findByIdWithAllDetails(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUpdatingNonExistentAnswer() {
        when(answerRepository.findById(999L)).thenReturn(Optional.empty());

        mockAnswer.setId(999L);

        assertThatThrownBy(() -> answerService.update(mockAnswer))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Resposta não encontrada para atualização!");
    }

    @Test
    void shouldFindAnswerById() {
        when(answerRepository.findByIdWithAllDetails(1L)).thenReturn(Optional.of(mockAnswer));

        AnswerEntity result = answerService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getComment()).isEqualTo("Test answer comment");
        verify(answerRepository).findByIdWithAllDetails(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAnswerNotFoundById() {
        when(answerRepository.findByIdWithAllDetails(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Resposta não encontrada!");
    }

    @Test
    void shouldFindAllAnswers() {
        List<AnswerEntity> answers = List.of(mockAnswer);
        when(answerRepository.findAll()).thenReturn(answers);

        List<AnswerEntity> result = answerService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(answerRepository).findAll();
    }

    @Test
    void shouldSaveCheckboxAnswerWithMultipleOptions() {
        mockQuestion.setType(TypeQuestionEnum.CHECKBOX);

        OptionEntity option1 = new OptionEntity();
        option1.setId(1L);
        OptionEntity option2 = new OptionEntity();
        option2.setId(2L);
        OptionEntity option3 = new OptionEntity();
        option3.setId(3L);

        Set<OptionEntity> options = new HashSet<>();
        options.add(option1);
        options.add(option2);
        options.add(option3);
        mockAnswer.setSelectedOptions(options);

        when(answerRepository.save(any(AnswerEntity.class))).thenReturn(mockAnswer);
        when(answerRepository.findByIdWithAllDetails(1L)).thenReturn(Optional.of(mockAnswer));

        AnswerEntity result = answerService.save(mockAnswer);

        assertThat(result).isNotNull();
        assertThat(result.getSelectedOptions()).hasSize(3);
        verify(answerRepository).save(mockAnswer);
        verify(answerRepository).findByIdWithAllDetails(1L);
    }
}
