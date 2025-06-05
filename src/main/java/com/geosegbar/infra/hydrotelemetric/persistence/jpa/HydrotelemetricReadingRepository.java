package com.geosegbar.infra.hydrotelemetric.persistence.jpa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.HydrotelemetricReadingEntity;

@Repository
public interface HydrotelemetricReadingRepository extends JpaRepository<HydrotelemetricReadingEntity, Long> {

    List<HydrotelemetricReadingEntity> findByDamIdOrderByDateDesc(Long damId);

    Page<HydrotelemetricReadingEntity> findByDamId(Long damId, Pageable pageable);

    List<HydrotelemetricReadingEntity> findByDamId(Long damId);

    List<HydrotelemetricReadingEntity> findByDamIdAndDateBetweenOrderByDateDesc(Long damId, LocalDate startDate, LocalDate endDate);

    Page<HydrotelemetricReadingEntity> findByDamIdAndDateBetween(Long damId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<HydrotelemetricReadingEntity> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);

    Page<HydrotelemetricReadingEntity> findByDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<HydrotelemetricReadingEntity> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);

    boolean existsByDamIdAndDate(Long damId, LocalDate date);
}
