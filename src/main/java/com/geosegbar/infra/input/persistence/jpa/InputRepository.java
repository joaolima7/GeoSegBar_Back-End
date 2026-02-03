package com.geosegbar.infra.input.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InputEntity;
import com.geosegbar.entities.InstrumentEntity;

@Repository
public interface InputRepository extends JpaRepository<InputEntity, Long> {

    @EntityGraph(attributePaths = {"instrument"})
    List<InputEntity> findByInstrumentId(Long instrumentId);

    @Override
    @EntityGraph(attributePaths = {"instrument"})
    Optional<InputEntity> findById(Long id);

    @EntityGraph(attributePaths = {"instrument"})
    List<InputEntity> findByInstrument(InstrumentEntity instrument);

    Optional<InputEntity> findByAcronymAndInstrumentId(String acronym, Long instrumentId);

    boolean existsByAcronymAndInstrumentId(String acronym, Long instrumentId);

    boolean existsByNameAndInstrumentId(String name, Long instrumentId);

    boolean existsByAcronymAndInstrumentIdAndIdNot(String acronym, Long instrumentId, Long id);

    boolean existsByNameAndInstrumentIdAndIdNot(String name, Long instrumentId, Long id);

    void deleteByInstrumentId(Long instrumentId);
}
