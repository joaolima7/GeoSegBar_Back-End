package com.geosegbar.infra.questionnaire_response.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.QuestionnaireResponseEntity;

@Repository
public interface QuestionnaireResponseRepository extends JpaRepository<QuestionnaireResponseEntity, Long> {

    @EntityGraph(attributePaths = {"templateQuestionnaire", "answers"})
    List<QuestionnaireResponseEntity> findByDamId(Long damId);

    @EntityGraph(attributePaths = {"templateQuestionnaire", "answers", "answers.question"})
    List<QuestionnaireResponseEntity> findByChecklistResponseId(Long checklistResponseId);

    @EntityGraph(attributePaths = {"templateQuestionnaire", "answers"})
    List<QuestionnaireResponseEntity> findByDamIdAndCreatedAtBetween(Long damId, LocalDateTime start, LocalDateTime end);

    boolean existsByTemplateQuestionnaireId(Long templateQuestionnaireId);

    @EntityGraph(attributePaths = {"templateQuestionnaire", "answers", "answers.selectedOptions", "answers.question", "answers.photos"})
    @Query("SELECT qr FROM QuestionnaireResponseEntity qr WHERE qr.checklistResponse.id = :checklistResponseId")
    List<QuestionnaireResponseEntity> findByChecklistResponseIdOptimized(@Param("checklistResponseId") Long checklistResponseId);

    @Query("SELECT qr FROM QuestionnaireResponseEntity qr "
            + "JOIN qr.answers a "
            + "JOIN a.selectedOptions o "
            + "WHERE qr.templateQuestionnaire.id = :templateId "
            + "AND a.question.id = :questionId "
            + "AND o.id = :optionId "
            + "ORDER BY qr.createdAt DESC")
    @EntityGraph(attributePaths = {"checklistResponse", "answers", "answers.selectedOptions"})
    List<QuestionnaireResponseEntity> findResponsesByTemplateQuestionAndOption(
            @Param("templateId") Long templateId,
            @Param("questionId") Long questionId,
            @Param("optionId") Long optionId);

    @Query("SELECT DISTINCT qr FROM QuestionnaireResponseEntity qr "
            + "WHERE qr.dam.id = :damId "
            + "ORDER BY qr.createdAt DESC")
    @EntityGraph(attributePaths = {"templateQuestionnaire", "answers", "answers.selectedOptions",
        "answers.question", "answers.photos", "checklistResponse"})
    List<QuestionnaireResponseEntity> findByDamIdWithDetails(@Param("damId") Long damId);

    @Query("SELECT qr FROM QuestionnaireResponseEntity qr "
            + "WHERE qr.dam.id = :damId "
            + "AND qr.templateQuestionnaire.id = :templateId "
            + "AND qr.createdAt = (SELECT MAX(qr2.createdAt) FROM QuestionnaireResponseEntity qr2 "
            + "                   WHERE qr2.dam.id = :damId AND qr2.templateQuestionnaire.id = :templateId)")
    @EntityGraph(attributePaths = {"answers", "answers.selectedOptions", "answers.question"})
    Optional<QuestionnaireResponseEntity> findLatestByDamIdAndTemplateId(
            @Param("damId") Long damId, @Param("templateId") Long templateId);

    @Override
    @EntityGraph(attributePaths = {"templateQuestionnaire", "answers", "answers.selectedOptions", "answers.question", "answers.photos"})
    List<QuestionnaireResponseEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"templateQuestionnaire", "answers", "answers.selectedOptions", "answers.question", "answers.photos"})
    Optional<QuestionnaireResponseEntity> findById(Long id);
}
