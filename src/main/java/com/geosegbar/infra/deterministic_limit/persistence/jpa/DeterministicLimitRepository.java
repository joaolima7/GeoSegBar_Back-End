package com.geosegbar.infra.deterministic_limit.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;

@Repository
public interface DeterministicLimitRepository extends JpaRepository<DeterministicLimitEntity, Long> {

    Optional<DeterministicLimitEntity> findByInstrumentId(Long instrumentId);

    Optional<DeterministicLimitEntity> findByInstrument(InstrumentEntity instrument);

    boolean existsByInstrumentId(Long instrumentId);

    void deleteByInstrumentId(Long instrumentId);
}
