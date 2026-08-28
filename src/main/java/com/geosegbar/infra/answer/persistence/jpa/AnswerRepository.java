package com.geosegbar.infra.answer.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnswerEntity;

@Repository
public interface AnswerRepository extends JpaRepository<AnswerEntity, Long> {

    @EntityGraph(attributePaths = {"question", "selectedOptions", "photos", "questionnaireResponse"})
    @Query("SELECT a FROM AnswerEntity a WHERE a.id = :id")
    Optional<AnswerEntity> findByIdWithAllDetails(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"question", "selectedOptions", "photos"})
    List<AnswerEntity> findAll();

    @EntityGraph(attributePaths = {"question", "selectedOptions", "photos"})
    @Query("SELECT a FROM AnswerEntity a WHERE a.questionnaireResponse.id = :questionnaireResponseId")
    List<AnswerEntity> findByQuestionnaireResponseIdWithDetails(@Param("questionnaireResponseId") Long questionnaireResponseId);

    @EntityGraph(attributePaths = {"selectedOptions", "photos", "questionnaireResponse"})
    @Query("SELECT a FROM AnswerEntity a WHERE a.question.id = :questionId")
    List<AnswerEntity> findByQuestionIdWithDetails(@Param("questionId") Long questionId);

    @EntityGraph(attributePaths = {"question", "selectedOptions", "photos",
        "questionnaireResponse", "questionnaireResponse.templateQuestionnaire"})
    @Query("SELECT a FROM AnswerEntity a WHERE a.id IN :answerIds " +
           "AND a.questionnaireResponse.checklistResponse.id = :checklistResponseId")
    List<AnswerEntity> findByIdsAndChecklistResponseId(
            @Param("answerIds") List<Long> answerIds,
            @Param("checklistResponseId") Long checklistResponseId);

    /**
     * Última resposta relevante de cada ponto (pergunta + questionário), para a
     * barragem — o mesmo valor que a API publica como {@code lastSelectedOption}
     * e sobre o qual a UI monta as opções disponíveis.
     *
     * Precisa casar EXATAMENTE com {@code findLatestNonNIAnswer}, que alimenta o
     * payload do checklist. Se as duas divergirem, a tela oferece uma opção que
     * o servidor recusa no envio, e o inspetor só descobre no fim.
     *
     * Duas decisões estão embutidas aqui, e são as mesmas da consulta de
     * leitura:
     *
     * - NI é ignorado. "Não Inspecionado" não é observação de campo: o ponto não
     * foi visto, então quem manda na transição é a última vez que ele foi.
     *
     * - Não filtra por checklist. O ponto é a pergunta dentro do questionário;
     * o histórico dele é o mesmo, seja qual for o checklist que o carrega.
     */
    @Query(value = """
            SELECT DISTINCT ON (a.question_id, qr.template_questionnaire_id)
                   a.question_id,
                   qr.template_questionnaire_id,
                   o.label
            FROM answers a
            JOIN answer_options ao ON ao.answer_id = a.id
            JOIN options o ON o.id = ao.option_id
            JOIN questionnaire_responses qr ON qr.id = a.questionnaire_response_id
            WHERE a.question_id IN :questionIds
              AND qr.template_questionnaire_id IN :templateIds
              AND qr.dam_id = :damId
              AND UPPER(o.label) <> 'NI'
            ORDER BY a.question_id, qr.template_questionnaire_id, qr.created_at DESC, qr.id DESC
            """, nativeQuery = true)
    List<Object[]> findLastRelevantOptionLabels(
            @Param("questionIds") List<Long> questionIds,
            @Param("templateIds") List<Long> templateIds,
            @Param("damId") Long damId);

    /**
     * Mesma regra de {@link #findLastRelevantOptionLabels}, restrita ao que
     * existia ANTES da inspeção sendo corrigida.
     *
     * Ao editar uma resposta já gravada, a transição precisa ser julgada contra
     * o que havia na época daquela inspeção — não contra o que veio depois.
     * Usar a "última resposta" aqui julgaria a correção contra o futuro dela.
     *
     * A fronteira exclui a própria resposta editada de duas formas
     * independentes: pelo ID do checklist response, que é exato, e pela data.
     * Só a data já bastaria hoje (o created_at do questionário é sempre
     * posterior ao do checklist response, porque é gravado depois na mesma
     * transação), mas essa é uma garantia acidental — a exclusão por ID não
     * depende de como os dois timestamps se ordenam.
     */
    @Query(value = """
            SELECT DISTINCT ON (a.question_id, qr.template_questionnaire_id)
                   a.question_id,
                   qr.template_questionnaire_id,
                   o.label
            FROM answers a
            JOIN answer_options ao ON ao.answer_id = a.id
            JOIN options o ON o.id = ao.option_id
            JOIN questionnaire_responses qr ON qr.id = a.questionnaire_response_id
            JOIN checklist_responses cr ON cr.id = qr.checklist_response_id
            WHERE a.question_id IN :questionIds
              AND qr.template_questionnaire_id IN :templateIds
              AND qr.dam_id = :damId
              AND UPPER(o.label) <> 'NI'
              AND cr.id <> :checklistResponseId
              AND cr.created_at < :beforeDate
            ORDER BY a.question_id, qr.template_questionnaire_id, qr.created_at DESC, qr.id DESC
            """, nativeQuery = true)
    List<Object[]> findRelevantOptionLabelsBefore(
            @Param("questionIds") List<Long> questionIds,
            @Param("templateIds") List<Long> templateIds,
            @Param("damId") Long damId,
            @Param("checklistResponseId") Long checklistResponseId,
            @Param("beforeDate") LocalDateTime beforeDate);

    @Query("SELECT a FROM AnswerEntity a "
            + "LEFT JOIN FETCH a.selectedOptions o "
            + "WHERE a.questionnaireResponse.dam.id = :damId "
            + "AND a.question.id = :questionId "
            + "AND (o IS NULL OR LOWER(o.label) != 'ni') "
            + "ORDER BY a.questionnaireResponse.createdAt DESC")
    @EntityGraph(attributePaths = {"question", "questionnaireResponse"})
    List<AnswerEntity> findNonNIAnswersByDamAndQuestion(
            @Param("damId") Long damId, @Param("questionId") Long questionId);

    @Query("""
        SELECT a FROM AnswerEntity a
        JOIN a.selectedOptions o
        WHERE a.questionnaireResponse.dam.id = :damId
        AND a.question.id = :questionId
        AND a.questionnaireResponse.templateQuestionnaire.id = :templateId
        AND LOWER(o.label) != 'ni'
        ORDER BY a.questionnaireResponse.createdAt DESC
        """)
    @EntityGraph(attributePaths = {"selectedOptions", "question", "questionnaireResponse"})
    List<AnswerEntity> findLatestNonNIAnswerOptimized(
            @Param("damId") Long damId,
            @Param("questionId") Long questionId,
            @Param("templateId") Long templateId,
            Pageable pageable);

    @Query("SELECT a FROM AnswerEntity a "
            + "JOIN a.selectedOptions o "
            + "WHERE a.questionnaireResponse.dam.id = :damId "
            + "AND a.question.id = :questionId "
            + "AND a.questionnaireResponse.templateQuestionnaire.id = :templateId "
            + "AND LOWER(o.label) != 'ni' "
            + "AND a.questionnaireResponse.createdAt = ("
            + "    SELECT MAX(qr.createdAt) FROM QuestionnaireResponseEntity qr "
            + "    JOIN qr.answers a2 "
            + "    JOIN a2.selectedOptions o2 "
            + "    WHERE qr.dam.id = :damId "
            + "    AND a2.question.id = :questionId "
            + "    AND qr.templateQuestionnaire.id = :templateId "
            + "    AND LOWER(o2.label) != 'ni')")
    @EntityGraph(attributePaths = {"selectedOptions", "question", "questionnaireResponse"})
    Optional<AnswerEntity> findLatestNonNIAnswer(
            @Param("damId") Long damId,
            @Param("questionId") Long questionId,
            @Param("templateId") Long templateId);
}
