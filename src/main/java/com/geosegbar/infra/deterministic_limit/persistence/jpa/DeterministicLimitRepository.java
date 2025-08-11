package com.geosegbar.infra.deterministic_limit.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DeterministicLimitEntity;

@Repository
public interface DeterministicLimitRepository extends JpaRepository<DeterministicLimitEntity, Long> {

    Optional<DeterministicLimitEntity> findByOutputId(Long outputId);

    @Query("SELECT dl.id FROM DeterministicLimitEntity dl WHERE dl.output.instrument.dam.id = :damId")
    List<Long> findLimitIdsByOutputInstrumentDamId(@Param("damId") Long damId);

}
