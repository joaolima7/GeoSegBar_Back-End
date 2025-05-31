package com.geosegbar.infra.constant.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.InstrumentEntity;

@Repository
public interface ConstantRepository extends JpaRepository<ConstantEntity, Long> {

    List<ConstantEntity> findByInstrumentId(Long instrumentId);

    List<ConstantEntity> findByInstrument(InstrumentEntity instrument);

    Optional<ConstantEntity> findByAcronymAndInstrumentId(String acronym, Long instrumentId);

    boolean existsByAcronymAndInstrumentId(String acronym, Long instrumentId);

    boolean existsByNameAndInstrumentId(String name, Long instrumentId);

    boolean existsByAcronymAndInstrumentIdAndIdNot(String acronym, Long instrumentId, Long id);

    boolean existsByNameAndInstrumentIdAndIdNot(String name, Long instrumentId, Long id);

    void deleteByInstrumentId(Long instrumentId);
}
