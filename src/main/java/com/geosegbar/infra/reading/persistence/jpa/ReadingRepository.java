package com.geosegbar.infra.reading.persistence.jpa;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.entities.ReadingEntity;

@Repository
public interface ReadingRepository extends JpaRepository<ReadingEntity, Long> {

    boolean existsByInstrumentIdAndDate(Long instrumentId, LocalDate date);

    List<ReadingEntity> findByInstrumentId(Long instrumentId);

    Page<ReadingEntity> findByInstrumentId(Long instrumentId, Pageable pageable);

    List<ReadingEntity> findByOutputId(Long outputId);

    Page<ReadingEntity> findByOutputId(Long outputId, Pageable pageable);

    List<ReadingEntity> findByInstrumentIdAndDateBetween(Long instrumentId, LocalDate startDate, LocalDate endDate);

    Page<ReadingEntity> findByInstrumentIdAndDateBetween(Long instrumentId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<ReadingEntity> findByInstrumentIdAndLimitStatus(Long instrumentId, LimitStatusEnum limitStatus);

    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id = :instrumentId AND r.active = true ORDER BY r.date DESC, r.hour DESC")
    List<ReadingEntity> findByInstrumentIdOrderByDateDescHourDesc(@Param("instrumentId") Long instrumentId);

    Page<ReadingEntity> findByInstrumentIdOrderByDateDescHourDesc(Long instrumentId, Pageable pageable);

    @Query("SELECT r FROM ReadingEntity r WHERE r.output.id = :outputId AND r.active = true ORDER BY r.date DESC, r.hour DESC")
    List<ReadingEntity> findByOutputIdOrderByDateDescHourDesc(@Param("outputId") Long outputId);

    Page<ReadingEntity> findByOutputIdOrderByDateDescHourDesc(Long outputId, Pageable pageable);

    List<ReadingEntity> findByInstrumentIdAndDateBetweenOrderByDateDescHourDesc(Long instrumentId, LocalDate startDate, LocalDate endDate);

    Page<ReadingEntity> findByInstrumentIdAndDateBetweenOrderByDateDescHourDesc(Long instrumentId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<ReadingEntity> findByInstrumentIdAndLimitStatusOrderByDateDescHourDesc(Long instrumentId, LimitStatusEnum limitStatus);

    @Query("SELECT r.date, r.hour FROM ReadingEntity r WHERE r.instrument.id = :instrumentId AND r.active = true GROUP BY r.date, r.hour ORDER BY r.date DESC, r.hour DESC")
    Page<Object[]> findDistinctDateHourByInstrumentId(@Param("instrumentId") Long instrumentId, Pageable pageable);

    List<ReadingEntity> findByInstrumentIdAndDateAndHourAndActiveTrue(Long instrumentId, LocalDate date, LocalTime hour);

    List<ReadingEntity> findByInstrumentIdAndDateAndHour(Long instrumentId, LocalDate date, LocalTime hour);

    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id = :instrumentId "
            + "AND r.date = :date AND r.hour = :hour AND r.user.id = :userId AND r.active = true")
    List<ReadingEntity> findByInstrumentAndDateAndHourAndUser(
            @Param("instrumentId") Long instrumentId,
            @Param("date") LocalDate date,
            @Param("hour") LocalTime hour,
            @Param("userId") Long userId);

    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id = :instrumentId "
            + "AND (:outputId IS NULL OR r.output.id = :outputId) "
            + "AND (:startDate IS NULL OR r.date >= :startDate) "
            + "AND (:endDate IS NULL OR r.date <= :endDate) "
            + "AND (:limitStatus IS NULL OR r.limitStatus = :limitStatus) "
            + "AND (:active IS NULL OR r.active = :active) "
            + "ORDER BY r.date DESC, r.hour DESC")
    Page<ReadingEntity> findByFilters(
            @Param("instrumentId") Long instrumentId,
            @Param("outputId") Long outputId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limitStatus") LimitStatusEnum limitStatus,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id = :instrumentId AND r.active = true ORDER BY r.date DESC, r.hour DESC")
    List<ReadingEntity> findTopNByInstrumentIdOrderByDateDescHourDesc(
            @Param("instrumentId") Long instrumentId, Pageable pageable);

    @Query("SELECT DISTINCT r.instrument.id FROM ReadingEntity r WHERE r.instrument.dam.client.id = :clientId AND r.active = true")
    List<Long> findDistinctInstrumentIdsByClientId(@Param("clientId") Long clientId);

    @EntityGraph(attributePaths = {"user", "instrument", "output", "inputValues"})
    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id = :instrumentId AND r.active = true ORDER BY r.date DESC, r.hour DESC")
    List<ReadingEntity> findByInstrumentIdOptimized(@Param("instrumentId") Long instrumentId);

    @EntityGraph(attributePaths = {"user", "instrument", "output", "inputValues"})
    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id = :instrumentId AND r.active = true ORDER BY r.date DESC, r.hour DESC")
    Page<ReadingEntity> findByInstrumentIdOptimized(@Param("instrumentId") Long instrumentId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "instrument", "output", "inputValues"})
    @Query("SELECT r FROM ReadingEntity r WHERE r.output.id = :outputId AND r.active = true ORDER BY r.date DESC, r.hour DESC")
    List<ReadingEntity> findByOutputIdOptimized(@Param("outputId") Long outputId);

    @EntityGraph(attributePaths = {"user", "instrument", "output", "inputValues"})
    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id = :instrumentId "
            + "AND (:outputId IS NULL OR r.output.id = :outputId) "
            + "AND (:startDate IS NULL OR r.date >= :startDate) "
            + "AND (:endDate IS NULL OR r.date <= :endDate) "
            + "AND (:limitStatus IS NULL OR r.limitStatus = :limitStatus) "
            + "AND (:active IS NULL OR r.active = :active) "
            + "ORDER BY r.date DESC, r.hour DESC")
    Page<ReadingEntity> findByFiltersOptimized(
            @Param("instrumentId") Long instrumentId,
            @Param("outputId") Long outputId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limitStatus") LimitStatusEnum limitStatus,
            @Param("active") Boolean active,
            Pageable pageable);

    @EntityGraph(attributePaths = {"user", "instrument", "output", "inputValues"})
    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id = :instrumentId AND r.active = true ORDER BY r.date DESC, r.hour DESC")
    List<ReadingEntity> findTopNByInstrumentIdOptimized(@Param("instrumentId") Long instrumentId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "instrument", "output", "inputValues"})
    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id IN :instrumentIds "
            + "AND (:startDate IS NULL OR r.date >= :startDate) "
            + "AND (:endDate IS NULL OR r.date <= :endDate) "
            + "AND (:limitStatus IS NULL OR r.limitStatus = :limitStatus) "
            + "AND (:active IS NULL OR r.active = :active) "
            + "ORDER BY r.date DESC, r.hour DESC")
    Page<ReadingEntity> findByMultipleInstrumentsWithFilters(
            @Param("instrumentIds") List<Long> instrumentIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limitStatus") LimitStatusEnum limitStatus,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("SELECT DISTINCT r.date, r.hour FROM ReadingEntity r WHERE r.instrument.id IN :instrumentIds AND r.active = true "
            + "ORDER BY r.date DESC, r.hour DESC")
    Page<Object[]> findDistinctDateHourByMultipleInstrumentIds(
            @Param("instrumentIds") List<Long> instrumentIds, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "instrument", "output", "inputValues"})
    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id IN :instrumentIds "
            + "AND r.date = :date AND r.hour = :hour AND r.active = true "
            + "ORDER BY r.instrument.id, r.output.id")
    List<ReadingEntity> findByMultipleInstrumentIdsAndDateAndHourAndActiveTrue(
            @Param("instrumentIds") List<Long> instrumentIds,
            @Param("date") LocalDate date,
            @Param("hour") LocalTime hour);

    @EntityGraph(attributePaths = {"user", "instrument", "output", "inputValues"})
    @Query("SELECT r FROM ReadingEntity r WHERE r.instrument.id = :instrumentId "
            + "AND r.active = true ORDER BY r.date DESC, r.hour DESC")
    List<ReadingEntity> findLatestReadingsByInstrumentId(
            @Param("instrumentId") Long instrumentId,
            Pageable pageable);

    @Query("SELECT DISTINCT o.instrument.id FROM OutputEntity o WHERE o.id IN :outputIds")
    Set<Long> findInstrumentIdsByOutputIds(@Param("outputIds") List<Long> outputIds);

    @Query(value = "SELECT r.id FROM reading r WHERE r.instrument_id = :instrumentId "
            + "AND r.active = true ORDER BY r.date DESC, r.hour DESC LIMIT :limit",
            nativeQuery = true)
    List<Long> findLatestReadingIdsByInstrumentId(
            @Param("instrumentId") Long instrumentId,
            @Param("limit") int limit);

    @Query(value = "SELECT r.id FROM reading r WHERE r.instrument_id = :instrumentId "
            + "AND r.date >= :startDate "
            + "AND r.active = true ORDER BY r.date DESC, r.hour DESC LIMIT :limit",
            nativeQuery = true)
    List<Long> findLatestReadingIdsByInstrumentIdAndStartDate(
            @Param("instrumentId") Long instrumentId,
            @Param("startDate") LocalDate startDate,
            @Param("limit") int limit);

    @Query(value = "SELECT r.id FROM reading r WHERE r.instrument_id = :instrumentId "
            + "AND r.date <= :endDate "
            + "AND r.active = true ORDER BY r.date DESC, r.hour DESC LIMIT :limit",
            nativeQuery = true)
    List<Long> findLatestReadingIdsByInstrumentIdAndEndDate(
            @Param("instrumentId") Long instrumentId,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit);

    @Query(value = "SELECT r.id FROM reading r WHERE r.instrument_id = :instrumentId "
            + "AND r.date >= :startDate AND r.date <= :endDate "
            + "AND r.active = true ORDER BY r.date DESC, r.hour DESC LIMIT :limit",
            nativeQuery = true)
    List<Long> findLatestReadingIdsByInstrumentIdAndDateRange(
            @Param("instrumentId") Long instrumentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit);

    @EntityGraph(attributePaths = {"user", "instrument", "output", "inputValues"})
    List<ReadingEntity> findByIdIn(List<Long> ids);
}
