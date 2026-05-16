package com.geosegbar.infra.checklist_template.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ChecklistTemplateEntity;

@Repository
public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplateEntity, Long> {

    @EntityGraph(attributePaths = {"templateQuestionnaire"})
    List<ChecklistTemplateEntity> findByChecklistIdOrderByOrderIndex(Long checklistId);

    @Query("SELECT COUNT(ct) FROM ChecklistTemplateEntity ct WHERE ct.checklist.id = :checklistId")
    long countByChecklistId(@Param("checklistId") Long checklistId);

    Optional<ChecklistTemplateEntity> findByChecklistIdAndTemplateQuestionnaireId(
            Long checklistId, Long templateQuestionnaireId);

    @Modifying
    @Query("UPDATE ChecklistTemplateEntity ct SET ct.orderIndex = :orderIndex WHERE ct.id = :id")
    void updateOrderIndex(@Param("id") Long id, @Param("orderIndex") Integer orderIndex);
}
