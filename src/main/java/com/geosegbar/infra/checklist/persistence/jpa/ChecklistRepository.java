package com.geosegbar.infra.checklist.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ChecklistEntity;

@Repository
public interface ChecklistRepository extends JpaRepository<ChecklistEntity, Long> {
    List<ChecklistEntity> findByDams_Id(Long damId);
}
