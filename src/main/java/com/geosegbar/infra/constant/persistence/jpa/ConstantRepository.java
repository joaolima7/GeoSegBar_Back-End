package com.geosegbar.infra.constant.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.InstrumentEntity;

import jakarta.persistence.QueryHint;

@Repository
public interface ConstantRepository extends JpaRepository<ConstantEntity, Long> {

    List<ConstantEntity> findByInstrumentId(Long instrumentId);

    List<ConstantEntity> findByInstrument(InstrumentEntity instrument);

    Optional<ConstantEntity> findByAcronymAndInstrumentId(String acronym, Long instrumentId);

    boolean existsByAcronymAndInstrumentId(String acronym, Long instrumentId);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT c.id FROM ConstantEntity c "
            + "WHERE c.instrument.dam.id = :damId")
    List<Long> findConstantIdsByInstrumentDamId(@Param("damId") Long damId);

    boolean existsByNameAndInstrumentId(String name, Long instrumentId);

    boolean existsByAcronymAndInstrumentIdAndIdNot(String acronym, Long instrumentId, Long id);

    boolean existsByNameAndInstrumentIdAndIdNot(String name, Long instrumentId, Long id);

    void deleteByInstrumentId(Long instrumentId);
}
