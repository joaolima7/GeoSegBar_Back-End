package com.geosegbar.infra.anomaly.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnomalyEntity;

@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEntity, Long> {

    List<AnomalyEntity> findByDamId(Long damId);

    List<AnomalyEntity> findByUserId(Long userId);

    List<AnomalyEntity> findByDamIdAndUserId(Long damId, Long userId);

    @Override
    @EntityGraph(attributePaths = {"photos"})
    List<AnomalyEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"photos"})
    Optional<AnomalyEntity> findById(Long id);

    @EntityGraph(attributePaths = {"photos"})
    List<AnomalyEntity> findWithPhotosByDamId(Long damId);
}
