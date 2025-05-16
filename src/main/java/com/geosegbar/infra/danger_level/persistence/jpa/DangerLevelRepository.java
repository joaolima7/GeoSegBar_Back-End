package com.geosegbar.infra.danger_level.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DangerLevelEntity;

@Repository
public interface DangerLevelRepository extends JpaRepository<DangerLevelEntity, Long> {

    Optional<DangerLevelEntity> findByName(String name);
}
