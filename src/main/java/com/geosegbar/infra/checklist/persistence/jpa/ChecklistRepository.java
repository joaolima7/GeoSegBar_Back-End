package com.geosegbar.infra.checklist.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ChecklistEntity;

@Repository
public interface ChecklistRepository extends JpaRepository<ChecklistEntity, Long> {

    @EntityGraph(attributePaths = {"templateQuestionnaires"})
    ChecklistEntity findByDamId(Long damId);

    @EntityGraph(attributePaths = {"templateQuestionnaires", "templateQuestionnaires.templateQuestions",
        "templateQuestionnaires.templateQuestions.question",
        "templateQuestionnaires.templateQuestions.question.options", "dam"})
    @Query("SELECT c FROM ChecklistEntity c WHERE c.dam.id = :damId")
    ChecklistEntity findByDamIdWithFullDetails(@Param("damId") Long damId);

    @EntityGraph(attributePaths = {"templateQuestionnaires", "templateQuestionnaires.templateQuestions",
        "templateQuestionnaires.templateQuestions.question",
        "templateQuestionnaires.templateQuestions.question.options", "dam"})
    @Query("SELECT c FROM ChecklistEntity c WHERE c.id = :id")
    Optional<ChecklistEntity> findByIdWithFullDetails(@Param("id") Long id);

    Optional<ChecklistEntity> findByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = {"dam"})
    @Query("SELECT c FROM ChecklistEntity c WHERE c.id = :checklistId")
    Optional<ChecklistEntity> findByIdWithDam(@Param("checklistId") Long checklistId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByNameAndDamId(String name, Long damId);

    boolean existsByNameAndDamIdAndIdNot(String name, Long damId, Long id);

    @EntityGraph(attributePaths = {"dam"})
    @Query("SELECT c FROM ChecklistEntity c")
    Page<ChecklistEntity> findAllWithDams(Pageable pageable);
}
