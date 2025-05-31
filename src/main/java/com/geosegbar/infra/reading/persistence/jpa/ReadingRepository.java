package com.geosegbar.infra.reading.persistence.jpa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.entities.ReadingEntity;

@Repository
public interface ReadingRepository extends JpaRepository<ReadingEntity, Long> {

    List<ReadingEntity> findByInstrumentId(Long instrumentId);

    Page<ReadingEntity> findByInstrumentId(Long instrumentId, Pageable pageable);

    List<ReadingEntity> findByInstrumentIdAndDateBetween(Long instrumentId, LocalDate startDate, LocalDate endDate);

    Page<ReadingEntity> findByInstrumentIdAndDateBetween(Long instrumentId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<ReadingEntity> findByInstrumentIdAndLimitStatus(Long instrumentId, LimitStatusEnum limitStatus);

    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id = :instrumentId "
            + "AND (:startDate IS NULL OR r.date >= :startDate) "
            + "AND (:endDate IS NULL OR r.date <= :endDate) "
            + "AND (:limitStatus IS NULL OR r.limitStatus = :limitStatus)")
    Page<ReadingEntity> findByFilters(
            @Param("instrumentId") Long instrumentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limitStatus") LimitStatusEnum limitStatus,
            Pageable pageable);
}
