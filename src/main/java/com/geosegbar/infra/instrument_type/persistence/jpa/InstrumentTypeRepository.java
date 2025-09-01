package com.geosegbar.infra.instrument_type.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentTypeEntity;

@Repository
public interface InstrumentTypeRepository extends JpaRepository<InstrumentTypeEntity, Long> {

    Optional<InstrumentTypeEntity> findByName(String name);

    boolean existsByName(String name);

    List<InstrumentTypeEntity> findAllByOrderByNameAsc();
}
