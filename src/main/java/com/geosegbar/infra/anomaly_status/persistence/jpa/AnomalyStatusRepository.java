package com.geosegbar.infra.anomaly_status.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnomalyStatusEntity;

@Repository
public interface AnomalyStatusRepository extends JpaRepository<AnomalyStatusEntity, Long> {

    Optional<AnomalyStatusEntity> findByName(String name);
}
