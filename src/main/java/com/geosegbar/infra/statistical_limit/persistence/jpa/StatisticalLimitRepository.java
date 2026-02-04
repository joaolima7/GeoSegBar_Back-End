package com.geosegbar.infra.statistical_limit.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.StatisticalLimitEntity;

@Repository
public interface StatisticalLimitRepository extends JpaRepository<StatisticalLimitEntity, Long> {

    @EntityGraph(attributePaths = {"output", "output.instrument"})
    Optional<StatisticalLimitEntity> findByOutputId(Long outputId);

    @Query("SELECT sl.id FROM StatisticalLimitEntity sl WHERE sl.output.instrument.dam.id = :damId")
    List<Long> findLimitIdsByOutputInstrumentDamId(@Param("damId") Long damId);
}
