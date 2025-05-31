package com.geosegbar.infra.statistical_limit.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.StatisticalLimitEntity;

@Repository
public interface StatisticalLimitRepository extends JpaRepository<StatisticalLimitEntity, Long> {

    Optional<StatisticalLimitEntity> findByInstrumentId(Long instrumentId);

    Optional<StatisticalLimitEntity> findByInstrument(InstrumentEntity instrument);

    boolean existsByInstrumentId(Long instrumentId);

    void deleteByInstrumentId(Long instrumentId);
}
