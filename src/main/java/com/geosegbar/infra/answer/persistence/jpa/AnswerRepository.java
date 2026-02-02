package com.geosegbar.infra.answer.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnswerEntity;

import jakarta.persistence.EntityManager;

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
