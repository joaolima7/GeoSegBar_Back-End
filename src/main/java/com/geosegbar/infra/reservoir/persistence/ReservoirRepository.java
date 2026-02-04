package com.geosegbar.infra.reservoir.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.ReservoirEntity;

@Repository
public interface ReservoirRepository extends JpaRepository<ReservoirEntity, Long> {

    @EntityGraph(attributePaths = {"dam", "level"})
    List<ReservoirEntity> findByDamOrderByCreatedAtDesc(DamEntity dam);

    @EntityGraph(attributePaths = {"dam", "level"})
    List<ReservoirEntity> findByDamIdOrderByCreatedAtDesc(Long damId);

    @Override
    @EntityGraph(attributePaths = {"dam", "level"})
    Optional<ReservoirEntity> findById(Long id);
}
