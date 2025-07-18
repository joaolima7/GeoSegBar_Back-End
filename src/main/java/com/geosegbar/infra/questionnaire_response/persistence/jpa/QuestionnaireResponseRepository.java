package com.geosegbar.infra.questionnaire_response.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.QuestionnaireResponseEntity;

@Repository
public interface QuestionnaireResponseRepository extends JpaRepository<QuestionnaireResponseEntity, Long> {

    List<QuestionnaireResponseEntity> findByDamId(Long damId);

    List<QuestionnaireResponseEntity> findByChecklistResponseId(Long checklistResponseId);

    List<QuestionnaireResponseEntity> findByDamIdAndCreatedAtBetween(Long damId, LocalDateTime start, LocalDateTime end);

    boolean existsByTemplateQuestionnaireId(Long templateQuestionnaireId);

    @EntityGraph(attributePaths = {"templateQuestionnaire", "answers", "answers.selectedOptions", "answers.question", "answers.photos"})
    @Query("SELECT qr FROM QuestionnaireResponseEntity qr WHERE qr.checklistResponse.id = :checklistResponseId")
    List<QuestionnaireResponseEntity> findByChecklistResponseIdOptimized(@Param("checklistResponseId") Long checklistResponseId);
}
