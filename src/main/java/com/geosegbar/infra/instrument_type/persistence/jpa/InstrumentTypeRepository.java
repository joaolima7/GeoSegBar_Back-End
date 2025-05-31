package com.geosegbar.infra.instrument_type.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentTypeEntity;

@Repository
public interface InstrumentTypeRepository extends JpaRepository<InstrumentTypeEntity, Long> {

    List<InstrumentTypeEntity> findAllByOrderByNameAsc();

    Optional<InstrumentTypeEntity> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
