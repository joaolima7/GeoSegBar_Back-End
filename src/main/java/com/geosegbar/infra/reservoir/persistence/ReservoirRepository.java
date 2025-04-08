package com.geosegbar.infra.reservoir.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.ReservoirEntity;

@Repository
public interface ReservoirRepository extends JpaRepository<ReservoirEntity, Long> {
    List<ReservoirEntity> findByDamOrderByCreatedAtDesc(DamEntity dam);
    List<ReservoirEntity> findByDamIdOrderByCreatedAtDesc(Long damId);
}
