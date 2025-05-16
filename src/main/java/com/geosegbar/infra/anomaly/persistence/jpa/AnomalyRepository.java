package com.geosegbar.infra.anomaly.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnomalyEntity;

@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEntity, Long> {

    List<AnomalyEntity> findByDamId(Long damId);

    List<AnomalyEntity> findByUserId(Long userId);

    List<AnomalyEntity> findByDamIdAndUserId(Long damId, Long userId);
}
