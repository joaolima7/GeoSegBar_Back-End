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
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

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

    @Mock
    private CacheManager checklistCacheManager;

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
        when(answerRepository.findById(1L)).thenReturn(Optional.of(mockAnswer));

        Cache mockCache = mock(Cache.class);
        when(checklistCacheManager.getCache(anyString())).thenReturn(mockCache);

        answerService.deleteById(1L);

        verify(answerRepository).deleteById(1L);
        verify(mockCache, times(6)).clear();
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDeletingNonExistentAnswer() {
        when(answerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.deleteById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Resposta não encontrada para exclusão!");
    }

    @Test
    void shouldSaveTextAnswerSuccessfully() {
        mockQuestion.setType(TypeQuestionEnum.TEXT);
        mockAnswer.setComment("Valid text answer");
        mockAnswer.setSelectedOptions(new HashSet<>());

        when(answerRepository.save(any(AnswerEntity.class))).thenReturn(mockAnswer);

        Cache mockCache = mock(Cache.class);
        when(checklistCacheManager.getCache(anyString())).thenReturn(mockCache);

        AnswerEntity result = answerService.save(mockAnswer);

        assertThat(result).isNotNull();
        assertThat(result.getComment()).isEqualTo("Valid text answer");
        verify(answerRepository).save(mockAnswer);
        verify(mockCache, times(6)).clear();
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

        Cache mockCache = mock(Cache.class);
        when(checklistCacheManager.getCache(anyString())).thenReturn(mockCache);

        AnswerEntity result = answerService.save(mockAnswer);

        assertThat(result).isNotNull();
        assertThat(result.getSelectedOptions()).hasSize(1);
        verify(answerRepository).save(mockAnswer);
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
        when(answerRepository.findById(1L)).thenReturn(Optional.of(mockAnswer));
        when(answerRepository.save(any(AnswerEntity.class))).thenReturn(mockAnswer);

        Cache mockCache = mock(Cache.class);
        when(checklistCacheManager.getCache(anyString())).thenReturn(mockCache);

        mockAnswer.setComment("Updated comment");

        AnswerEntity result = answerService.update(mockAnswer);

        assertThat(result).isNotNull();
        verify(answerRepository).save(mockAnswer);
        verify(mockCache, times(6)).clear();
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
        when(answerRepository.findById(1L)).thenReturn(Optional.of(mockAnswer));

        AnswerEntity result = answerService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getComment()).isEqualTo("Test answer comment");
        verify(answerRepository).findById(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAnswerNotFoundById() {
        when(answerRepository.findById(999L)).thenReturn(Optional.empty());

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
    void shouldEvictAllCachesWhenSavingAnswer() {
        mockQuestion.setType(TypeQuestionEnum.TEXT);
        mockAnswer.setComment("Valid text");

        Cache mockCache = mock(Cache.class);
        when(checklistCacheManager.getCache("checklistResponseById")).thenReturn(mockCache);
        when(checklistCacheManager.getCache("checklistResponseDetail")).thenReturn(mockCache);
        when(checklistCacheManager.getCache("checklistResponsesByDam")).thenReturn(mockCache);
        when(checklistCacheManager.getCache("checklistResponsesByUser")).thenReturn(mockCache);
        when(checklistCacheManager.getCache("checklistsWithAnswersByDam")).thenReturn(mockCache);
        when(checklistCacheManager.getCache("checklistsWithAnswersByClient")).thenReturn(mockCache);
        when(answerRepository.save(any(AnswerEntity.class))).thenReturn(mockAnswer);

        answerService.save(mockAnswer);

        verify(mockCache, times(6)).clear();
    }

    @Test
    void shouldEvictCachesByPatternWhenDeleting() {
        when(answerRepository.findById(1L)).thenReturn(Optional.of(mockAnswer));

        Cache mockCache = mock(Cache.class);
        when(checklistCacheManager.getCache(anyString())).thenReturn(mockCache);

        answerService.deleteById(1L);
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

        Cache mockCache = mock(Cache.class);
        when(checklistCacheManager.getCache(anyString())).thenReturn(mockCache);

        AnswerEntity result = answerService.save(mockAnswer);

        assertThat(result).isNotNull();
        assertThat(result.getSelectedOptions()).hasSize(3);
        verify(answerRepository).save(mockAnswer);
    }

    @Test
    void shouldHandleNullCacheGracefully() {
        mockQuestion.setType(TypeQuestionEnum.TEXT);
        mockAnswer.setComment("Valid text");

        when(checklistCacheManager.getCache(anyString())).thenReturn(null);
        when(answerRepository.save(any(AnswerEntity.class))).thenReturn(mockAnswer);

        AnswerEntity result = answerService.save(mockAnswer);

        assertThat(result).isNotNull();
        verify(answerRepository).save(mockAnswer);
    }
}
