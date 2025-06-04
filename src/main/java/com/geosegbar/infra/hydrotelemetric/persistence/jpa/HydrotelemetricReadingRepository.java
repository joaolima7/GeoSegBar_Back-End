package com.geosegbar.infra.hydrotelemetric.persistence.jpa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.HydrotelemetricReadingEntity;

@Repository
public interface HydrotelemetricReadingRepository extends JpaRepository<HydrotelemetricReadingEntity, Long> {

    List<HydrotelemetricReadingEntity> findByDamId(Long damId);

    List<HydrotelemetricReadingEntity> findByDamIdAndDateBetween(Long damId, LocalDate startDate, LocalDate endDate);

    List<HydrotelemetricReadingEntity> findByDamIdOrderByDateDesc(Long damId);

    List<HydrotelemetricReadingEntity> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);
}
