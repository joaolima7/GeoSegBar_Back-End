package com.geosegbar.infra.output.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;

@Repository
public interface OutputRepository extends JpaRepository<OutputEntity, Long> {

    List<OutputEntity> findByInstrumentIdAndActiveTrue(Long instrumentId);

    List<OutputEntity> findByInstrument(InstrumentEntity instrument);

    Optional<OutputEntity> findByAcronymAndInstrumentId(String acronym, Long instrumentId);

    boolean existsByAcronymAndInstrumentId(String acronym, Long instrumentId);

    boolean existsByNameAndInstrumentId(String name, Long instrumentId);

    boolean existsByAcronymAndInstrumentIdAndIdNot(String acronym, Long instrumentId, Long id);

    boolean existsByNameAndInstrumentIdAndIdNot(String name, Long instrumentId, Long id);

    void deleteByInstrumentId(Long instrumentId);

    @Query("SELECT o.id FROM OutputEntity o WHERE o.instrument.dam.id = :damId")
    List<Long> findOutputIdsByInstrumentDamId(@Param("damId") Long damId);
}
