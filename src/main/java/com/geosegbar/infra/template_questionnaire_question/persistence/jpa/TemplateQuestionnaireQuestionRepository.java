package com.geosegbar.infra.template_questionnaire_question.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;

@Repository
public interface TemplateQuestionnaireQuestionRepository extends JpaRepository<TemplateQuestionnaireQuestionEntity, Long>{
    
}
