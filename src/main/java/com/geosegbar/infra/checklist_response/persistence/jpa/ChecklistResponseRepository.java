package com.geosegbar.infra.checklist_response.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ChecklistResponseEntity;

@Repository
public interface ChecklistResponseRepository extends JpaRepository<ChecklistResponseEntity, Long> {
    List<ChecklistResponseEntity> findByDamId(Long damId);
    List<ChecklistResponseEntity> findByUserId(Long userId);
    List<ChecklistResponseEntity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    Page<ChecklistResponseEntity> findByDamId(Long damId, Pageable pageable);
    Page<ChecklistResponseEntity> findByUserId(Long userId, Pageable pageable);
    Page<ChecklistResponseEntity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    Page<ChecklistResponseEntity> findAll(Pageable pageable);
}
