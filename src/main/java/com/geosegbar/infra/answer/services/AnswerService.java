package com.geosegbar.infra.answer.services;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerService {

    private final AnswerRepository answerRepository;

    @Transactional
    public void deleteById(Long id) {

        if (!answerRepository.existsById(id)) {
            throw new NotFoundException("Resposta não encontrada para exclusão!");
        }
        answerRepository.deleteById(id);
        log.info("Answer {} deletado.", id);
    }

    @Transactional
    public AnswerEntity save(AnswerEntity answer) {
        validateAnswerByType(answer);
        AnswerEntity saved = answerRepository.save(answer);
        log.info("Answer {} criado.", saved.getId());

        return findById(saved.getId());
    }

    @Transactional
    public AnswerEntity update(AnswerEntity answer) {

        AnswerEntity existing = answerRepository.findById(answer.getId())
                .orElseThrow(() -> new NotFoundException("Resposta não encontrada para atualização!"));

        validateAnswerByType(answer);

        existing.setComment(answer.getComment());
        existing.setSelectedOptions(answer.getSelectedOptions());

        AnswerEntity saved = answerRepository.save(existing);
        log.info("Answer {} atualizado.", saved.getId());

        return findById(saved.getId());
    }

    /**
     * ✅ OTIMIZADO: Usa findByIdWithAllDetails Garante que selectedOptions e
     * photos venham carregados.
     */
    public AnswerEntity findById(Long id) {
        return answerRepository.findByIdWithAllDetails(id)
                .orElseThrow(() -> new NotFoundException("Resposta não encontrada!"));
    }

    /**
     * ✅ OTIMIZADO: Repository agora usa EntityGraph Cuidado: Retornar listas
     * gigantes inteiras é ruim, mas agora pelo menos não dará N+1.
     */
    public List<AnswerEntity> findAll() {
        return answerRepository.findAll();
    }

    public AnswerEntity findLatestAnswer(Long damId, Long questionId, Long templateId) {
        List<AnswerEntity> results = answerRepository.findLatestNonNIAnswerOptimized(
                damId, questionId, templateId, PageRequest.of(0, 1));

        return results.isEmpty() ? null : results.get(0);
    }

    private void validateAnswerByType(AnswerEntity answer) {
        QuestionEntity question = answer.getQuestion();

        if (question == null) {
            throw new InvalidInputException("A resposta deve estar associada a uma pergunta");
        }

        if (TypeQuestionEnum.TEXT.equals(question.getType())) {
            if (answer.getComment() == null || answer.getComment().trim().isEmpty()) {
                throw new InvalidInputException("Respostas para perguntas do tipo TEXTO devem ter o campo de texto preenchido!");
            }
            if (answer.getSelectedOptions() != null && !answer.getSelectedOptions().isEmpty()) {
                throw new InvalidInputException("Respostas para perguntas do tipo TEXTO não devem ter opções selecionadas!");
            }
        } else if (TypeQuestionEnum.CHECKBOX.equals(question.getType())) {
            if (answer.getSelectedOptions() == null || answer.getSelectedOptions().isEmpty()) {
                throw new InvalidInputException("Respostas para perguntas do tipo CHECKBOX devem ter pelo menos uma opção selecionada!");
            }
        }
    }
}
