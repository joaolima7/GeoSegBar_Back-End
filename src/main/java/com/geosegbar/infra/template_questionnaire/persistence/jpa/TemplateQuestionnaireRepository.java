package com.geosegbar.infra.template_questionnaire.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.TemplateQuestionnaireEntity;

@Repository
public interface TemplateQuestionnaireRepository extends JpaRepository<TemplateQuestionnaireEntity, Long> {
    List<TemplateQuestionnaireEntity> findByChecklistsId(Long checklistId);
}
