package com.geosegbar.infra.anomaly.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnomalyEntity;

@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEntity, Long> {

    @EntityGraph(value = "anomaly.complete")
    List<AnomalyEntity> findByUserId(Long userId);

    @EntityGraph(value = "anomaly.complete")
    List<AnomalyEntity> findByDamIdAndUserId(Long damId, Long userId);

    @Override
    @EntityGraph(value = "anomaly.complete")
    List<AnomalyEntity> findAll();

    @Override
    @EntityGraph(value = "anomaly.complete")
    Optional<AnomalyEntity> findById(Long id);

    @EntityGraph(value = "anomaly.complete")
    List<AnomalyEntity> findByDamId(Long damId);
}
