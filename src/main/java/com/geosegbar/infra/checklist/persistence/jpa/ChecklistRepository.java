package com.geosegbar.infra.checklist.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ChecklistEntity;

@Repository
public interface ChecklistRepository extends JpaRepository<ChecklistEntity, Long> {

    List<ChecklistEntity> findByDams_Id(Long damId);

    Optional<ChecklistEntity> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByNameAndDams_Id(String name, Long damId);

    boolean existsByNameAndDams_IdAndIdNot(String name, Long damId, Long id);
}
