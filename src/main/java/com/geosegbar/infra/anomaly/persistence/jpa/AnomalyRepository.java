package com.geosegbar.infra.anomaly.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnomalyEntity;

@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEntity, Long> {

    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findByDamIdAndUserId(Long damId, Long userId);

    @Override
    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    Optional<AnomalyEntity> findById(Long id);

    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findByDamId(Long damId);
}
