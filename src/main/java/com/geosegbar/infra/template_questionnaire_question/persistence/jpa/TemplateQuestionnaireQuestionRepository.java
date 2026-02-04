package com.geosegbar.infra.template_questionnaire_question.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;

@Repository
public interface TemplateQuestionnaireQuestionRepository extends JpaRepository<TemplateQuestionnaireQuestionEntity, Long> {

    @EntityGraph(attributePaths = {"question", "question.options", "templateQuestionnaire"})
    List<TemplateQuestionnaireQuestionEntity> findByTemplateQuestionnaireIdOrderByOrderIndex(Long templateQuestionnaireId);

    @EntityGraph(attributePaths = {"templateQuestionnaire"})
    List<TemplateQuestionnaireQuestionEntity> findByQuestionId(Long questionId);

    boolean existsByQuestionId(Long questionId);

    @Query("SELECT COUNT(tq) FROM TemplateQuestionnaireQuestionEntity tq WHERE tq.templateQuestionnaire.id = :templateId")
    int countQuestionsByTemplateId(@Param("templateId") Long templateId);

    @Override
    @EntityGraph(attributePaths = {"question", "question.options", "templateQuestionnaire"})
    Optional<TemplateQuestionnaireQuestionEntity> findById(Long id);
}
