package com.geosegbar.infra.checklist_response.persistence.jpa;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ChecklistResponseEntity;

@Repository
public interface ChecklistResponseRepository extends JpaRepository<ChecklistResponseEntity, Long> {

    @EntityGraph(attributePaths = {"user", "dam"})
    List<ChecklistResponseEntity> findByDamId(Long damId);

    @EntityGraph(attributePaths = {"user", "dam"})
    List<ChecklistResponseEntity> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "dam"})
    List<ChecklistResponseEntity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @EntityGraph(attributePaths = {"user", "dam", "questionnaireResponses",
        "questionnaireResponses.templateQuestionnaire",
        "questionnaireResponses.answers",
        "questionnaireResponses.answers.question",
        "questionnaireResponses.answers.selectedOptions",
        "questionnaireResponses.answers.photos"})
    @Query("SELECT cr FROM ChecklistResponseEntity cr WHERE cr.id = :id")
    Optional<ChecklistResponseEntity> findByIdWithFullDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "dam", "questionnaireResponses",
        "questionnaireResponses.templateQuestionnaire"})
    @Query("SELECT cr FROM ChecklistResponseEntity cr WHERE cr.dam.id = :damId AND cr.checklistId = :checklistId ORDER BY cr.createdAt DESC")
    List<ChecklistResponseEntity> findByDamIdAndChecklistIdWithDetailsOrderByCreatedAtDesc(
            @Param("damId") Long damId, @Param("checklistId") Long checklistId);

    @EntityGraph(attributePaths = {"user", "dam"})
    Page<ChecklistResponseEntity> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "dam"})
    Page<ChecklistResponseEntity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "dam"})
    @Override
    Page<ChecklistResponseEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "dam"})
    Page<ChecklistResponseEntity> findByDamIdIn(Collection<Long> damIds, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "dam"})
    List<ChecklistResponseEntity> findByDamIdOrderByCreatedAtDesc(Long damId);

    @EntityGraph(attributePaths = {"user", "dam"})
    @Query("SELECT cr FROM ChecklistResponseEntity cr WHERE cr.dam.id = :damId ORDER BY cr.createdAt DESC")
    Page<ChecklistResponseEntity> findByDamIdOptimized(@Param("damId") Long damId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "dam"})
    @Query("SELECT cr FROM ChecklistResponseEntity cr WHERE cr.dam.id IN :damIds ORDER BY cr.createdAt DESC")
    Page<ChecklistResponseEntity> findByDamIdsOptimized(@Param("damIds") Collection<Long> damIds, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "dam"})
    @Query("SELECT cr FROM ChecklistResponseEntity cr WHERE cr.dam.id = :damId ORDER BY cr.createdAt DESC")
    List<ChecklistResponseEntity> findByDamIdOptimizedList(@Param("damId") Long damId);

    @EntityGraph(attributePaths = {"user", "dam"})
    @Query("SELECT cr FROM ChecklistResponseEntity cr "
            + "JOIN cr.dam d "
            + "WHERE d.client.id = :clientId "
            + "ORDER BY cr.createdAt DESC")
    Page<ChecklistResponseEntity> findByClientIdOptimized(@Param("clientId") Long clientId, Pageable pageable);

    @Query("SELECT cr FROM ChecklistResponseEntity cr "
            + "WHERE cr.dam.id = :damId "
            + "AND cr.createdAt = (SELECT MAX(cr2.createdAt) FROM ChecklistResponseEntity cr2 WHERE cr2.dam.id = :damId)")
    @EntityGraph(attributePaths = {"user", "dam"})
    Optional<ChecklistResponseEntity> findLatestByDamId(@Param("damId") Long damId);

    @Query("SELECT cr FROM ChecklistResponseEntity cr "
            + "WHERE cr.dam.client.id = :clientId "
            + "AND cr.dam.id = :damId "
            + "AND cr.createdAt = (SELECT MAX(cr2.createdAt) FROM ChecklistResponseEntity cr2 "
            + "                   WHERE cr2.dam.client.id = :clientId AND cr2.dam.id = :damId)")
    @EntityGraph(attributePaths = {"user", "dam"})
    Optional<ChecklistResponseEntity> findLatestByClientIdAndDamId(
            @Param("clientId") Long clientId, @Param("damId") Long damId);

    @Query(value = """
    WITH client_dams AS (
        SELECT d.id AS dam_id
        FROM dam d
        WHERE d.client_id = :clientId
    ),
    unique_checklists AS (
        SELECT DISTINCT cr.checklist_id, cr.checklist_name
        FROM checklist_responses cr
        JOIN client_dams cd ON cr.dam_id = cd.dam_id
    ),
    latest_responses AS (
        SELECT cr.id, cr.checklist_id, cr.checklist_name,
               ROW_NUMBER() OVER (PARTITION BY cr.checklist_id ORDER BY cr.created_at DESC) as row_num
        FROM checklist_responses cr
        JOIN client_dams cd ON cr.dam_id = cd.dam_id
    )
    SELECT id FROM latest_responses
    WHERE row_num <= :limit
    ORDER BY checklist_id, row_num
    """, nativeQuery = true)
    List<Long> findLatestChecklistResponseIdsByClientIdAndLimit(
            @Param("clientId") Long clientId,
            @Param("limit") int limit);

    @EntityGraph(attributePaths = {"user", "dam"})
    List<ChecklistResponseEntity> findByIdIn(Collection<Long> ids);
}
