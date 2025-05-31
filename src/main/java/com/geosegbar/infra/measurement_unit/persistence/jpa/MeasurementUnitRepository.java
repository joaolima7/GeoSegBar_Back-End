package com.geosegbar.infra.measurement_unit.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.MeasurementUnitEntity;

@Repository
public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnitEntity, Long> {

    List<MeasurementUnitEntity> findAllByOrderByNameAsc();

    Optional<MeasurementUnitEntity> findByName(String name);

    Optional<MeasurementUnitEntity> findByAcronym(String acronym);

    boolean existsByName(String name);

    boolean existsByAcronym(String acronym);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByAcronymAndIdNot(String acronym, Long id);
}
