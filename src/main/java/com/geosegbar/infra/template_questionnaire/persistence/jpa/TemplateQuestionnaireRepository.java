package com.geosegbar.infra.template_questionnaire.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.TemplateQuestionnaireEntity;

@Repository
public interface TemplateQuestionnaireRepository extends JpaRepository<TemplateQuestionnaireEntity, Long> {

    @EntityGraph(attributePaths = {"templateQuestions", "templateQuestions.question", "templateQuestions.question.options"})
    List<TemplateQuestionnaireEntity> findByChecklistsId(Long checklistId);

    @EntityGraph(attributePaths = {"templateQuestions", "templateQuestions.question", "templateQuestions.question.options"})
    @Query("SELECT t FROM TemplateQuestionnaireEntity t WHERE t.id = :id")
    Optional<TemplateQuestionnaireEntity> findByIdWithFullDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"templateQuestions", "templateQuestions.question", "templateQuestions.question.options"})
    @Query("SELECT t FROM TemplateQuestionnaireEntity t")
    List<TemplateQuestionnaireEntity> findAllWithFullDetails();

    @EntityGraph(attributePaths = {"templateQuestions", "templateQuestions.question", "templateQuestions.question.options"})
    @Query("SELECT t FROM TemplateQuestionnaireEntity t WHERE t.id IN :ids")
    List<TemplateQuestionnaireEntity> findByIdsWithFullDetails(@Param("ids") List<Long> ids);
}
