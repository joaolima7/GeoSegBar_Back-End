package com.geosegbar.infra.reading.persistence.jpa;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.entities.ReadingEntity;
import com.geosegbar.infra.reading.projections.InstrumentLimitStatusProjection;

@Repository
public interface ReadingRepository extends JpaRepository<ReadingEntity, Long> {

    // ==========================================
    // VERIFICAÇÕES DE EXISTÊNCIA
    // ==========================================
    boolean existsByInstrumentIdAndDate(Long instrumentId, LocalDate date);

    boolean existsByInstrumentIdAndDateAndHourAndActive(
            Long instrumentId, LocalDate date, LocalTime hour, Boolean active);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM ReadingEntity r
            WHERE r.instrument.id = :instrumentId
              AND r.date = :date
              AND r.hour = :hour
              AND r.active = true
              AND r.id != :excludeId
            """)
    boolean existsByInstrumentIdAndDateAndHourExcludingId(
            @Param("instrumentId") Long instrumentId,
            @Param("date") LocalDate date,
            @Param("hour") LocalTime hour,
            @Param("excludeId") Long excludeId);

    // ==========================================
    // BUSCA POR ID COM RELAÇÕES
    // ==========================================
    @Query("""
            SELECT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH i.dam d
            LEFT JOIN FETCH d.client
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.id = :id
            """)
    Optional<ReadingEntity> findByIdWithAllRelations(@Param("id") Long id);

    // ==========================================
    // BUSCA POR INSTRUMENTO
    // ==========================================
    @Query("""
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.instrument.id = :instrumentId AND r.active = true
            ORDER BY r.date DESC, r.hour DESC
            """)
    List<ReadingEntity> findByInstrumentIdWithAllRelations(@Param("instrumentId") Long instrumentId);

    @Query(value = """
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.instrument.id = :instrumentId AND r.active = true
            ORDER BY r.date DESC, r.hour DESC
            """,
            countQuery = "SELECT COUNT(r) FROM ReadingEntity r WHERE r.instrument.id = :instrumentId AND r.active = true")
    Page<ReadingEntity> findByInstrumentIdWithAllRelations(@Param("instrumentId") Long instrumentId, Pageable pageable);

    @Query("""
            SELECT r FROM ReadingEntity r
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.instrument.id = :instrumentId AND r.active = true
            ORDER BY r.date DESC, r.hour DESC
            """)
    List<ReadingEntity> findTopNByInstrumentIdOptimized(@Param("instrumentId") Long instrumentId, Pageable pageable);

    // ==========================================
    // BUSCA POR OUTPUT
    // ==========================================
    @Query("""
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.output.id = :outputId AND r.active = true
            ORDER BY r.date DESC, r.hour DESC
            """)
    List<ReadingEntity> findByOutputIdWithAllRelations(@Param("outputId") Long outputId);

    // ==========================================
    // BUSCA POR MÚLTIPLOS INSTRUMENTOS
    // ==========================================
    @Query(value = """
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.instrument.id IN :instrumentIds
              AND (CAST(:startDate AS date) IS NULL OR r.date >= :startDate)
              AND (CAST(:endDate AS date) IS NULL OR r.date <= :endDate)
              AND (:limitStatus IS NULL OR r.limitStatus = :limitStatus)
              AND r.active = :active
            ORDER BY r.date DESC, r.hour DESC
            """,
            countQuery = """
                SELECT COUNT(DISTINCT r) FROM ReadingEntity r
                WHERE r.instrument.id IN :instrumentIds
                  AND (CAST(:startDate AS date) IS NULL OR r.date >= :startDate)
                  AND (CAST(:endDate AS date) IS NULL OR r.date <= :endDate)
                  AND (:limitStatus IS NULL OR r.limitStatus = :limitStatus)
                  AND r.active = :active
                """)
    Page<ReadingEntity> findByMultipleInstrumentsWithAllRelations(
            @Param("instrumentIds") List<Long> instrumentIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limitStatus") LimitStatusEnum limitStatus,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.instrument.id IN :instrumentIds AND r.active = true
            ORDER BY r.date DESC, r.hour DESC
            """)
    List<ReadingEntity> findByInstrumentIdsAndActiveTrueWithAllRelations(
            @Param("instrumentIds") List<Long> instrumentIds);

    // ==========================================
    // BUSCA COM FILTROS
    // ==========================================
    @Query(value = """
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.instrument.id = :instrumentId
              AND (:outputId IS NULL OR r.output.id = :outputId)
              AND (CAST(:startDate AS date) IS NULL OR r.date >= :startDate)
              AND (CAST(:endDate AS date) IS NULL OR r.date <= :endDate)
              AND (:limitStatus IS NULL OR r.limitStatus = :limitStatus)
              AND r.active = :active
            ORDER BY r.date DESC, r.hour DESC
            """,
            countQuery = """
                SELECT COUNT(DISTINCT r) FROM ReadingEntity r
                WHERE r.instrument.id = :instrumentId
                  AND (:outputId IS NULL OR r.output.id = :outputId)
                  AND (CAST(:startDate AS date) IS NULL OR r.date >= :startDate)
                  AND (CAST(:endDate AS date) IS NULL OR r.date <= :endDate)
                  AND (:limitStatus IS NULL OR r.limitStatus = :limitStatus)
                  AND r.active = :active
                """)
    Page<ReadingEntity> findByFiltersWithAllRelations(
            @Param("instrumentId") Long instrumentId,
            @Param("outputId") Long outputId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limitStatus") LimitStatusEnum limitStatus,
            @Param("active") Boolean active,
            Pageable pageable);

    // ==========================================
    // BUSCA AGRUPADA POR DATA/HORA
    // ==========================================
    @Query(value = """
            SELECT r.date, r.hour
            FROM ReadingEntity r
            WHERE r.instrument.id = :instrumentId
              AND (:active IS NULL OR r.active = :active)
            GROUP BY r.date, r.hour
            ORDER BY r.date DESC, r.hour DESC
            """,
            countQuery = """
                SELECT COUNT(DISTINCT CONCAT(r.date, r.hour))
                FROM ReadingEntity r
                WHERE r.instrument.id = :instrumentId
                  AND (:active IS NULL OR r.active = :active)
                """)
    Page<Object[]> findDistinctDateHourByInstrumentIdAndActive(
            @Param("instrumentId") Long instrumentId,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query(value = """
            SELECT DISTINCT r.date, r.hour
            FROM ReadingEntity r
            WHERE r.instrument.id IN :instrumentIds AND r.active = true
            ORDER BY r.date DESC, r.hour DESC
            """,
            countQuery = """
                SELECT COUNT(DISTINCT CONCAT(r.date, '_', r.hour))
                FROM ReadingEntity r
                WHERE r.instrument.id IN :instrumentIds AND r.active = true
                """)
    Page<Object[]> findDistinctDateHourByMultipleInstrumentIds(
            @Param("instrumentIds") List<Long> instrumentIds,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.instrument.id = :instrumentId
              AND r.date IN :dates
              AND r.hour IN :hours
              AND (:active IS NULL OR r.active = :active)
            ORDER BY r.date DESC, r.hour DESC
            """)
    List<ReadingEntity> findByInstrumentIdAndDateHoursWithAllRelations(
            @Param("instrumentId") Long instrumentId,
            @Param("dates") List<LocalDate> dates,
            @Param("hours") List<LocalTime> hours,
            @Param("active") Boolean active);

    @Query("""
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.instrument.id IN :instrumentIds
              AND r.date IN :dates
              AND r.hour IN :hours
              AND r.active = true
            ORDER BY r.date DESC, r.hour DESC
            """)
    List<ReadingEntity> findByMultipleInstrumentIdsAndDateHoursWithAllRelations(
            @Param("instrumentIds") List<Long> instrumentIds,
            @Param("dates") List<LocalDate> dates,
            @Param("hours") List<LocalTime> hours);

    // ==========================================
    // BUSCA PARA LIMIT STATUS - QUERY OTIMIZADA COM WINDOW FUNCTION
    // ==========================================
    @Query(value = """
            WITH ranked_readings AS (
                SELECT
                    r.id,
                    r.instrument_id,
                    r.date,
                    r.hour,
                    r.limit_status,
                    i.name as instrument_name,
                    it.name as instrument_type_name,
                    it.id as instrument_type_id,
                    d.id as dam_id,
                    d.name as dam_name,
                    c.id as client_id,
                    c.name as client_name,
                    ROW_NUMBER() OVER (
                        PARTITION BY r.instrument_id
                        ORDER BY r.date DESC, r.hour DESC
                    ) as rn
                FROM reading r
                INNER JOIN instrument i ON r.instrument_id = i.id
                INNER JOIN instrument_type it ON i.instrument_type_id = it.id
                INNER JOIN dam d ON i.dam_id = d.id
                INNER JOIN client c ON d.client_id = c.id
                WHERE c.id = :clientId
                  AND i.active = true
                  AND r.active = true
            )
            SELECT
                instrument_id as instrumentId,
                instrument_name as instrumentName,
                instrument_type_name as instrumentTypeName,
                instrument_type_id as instrumentTypeId,
                dam_id as damId,
                dam_name as damName,
                client_id as clientId,
                client_name as clientName,
                date as readingDate,
                hour as readingHour,
                limit_status as limitStatus
            FROM ranked_readings
            WHERE rn <= :limit
            ORDER BY instrument_id, rn
            """, nativeQuery = true)
    List<InstrumentLimitStatusProjection> findLatestLimitStatusByClientId(
            @Param("clientId") Long clientId,
            @Param("limit") int limit);

    // ==========================================
    // BUSCA LATEST DATE/HOURS POR CLIENTE
    // ==========================================
    @Query(value = """
            WITH instrument_date_hours AS (
                SELECT
                    r.instrument_id,
                    r.date,
                    r.hour,
                    DENSE_RANK() OVER (
                        PARTITION BY r.instrument_id
                        ORDER BY r.date DESC, r.hour DESC
                    ) as date_hour_rank
                FROM reading r
                INNER JOIN instrument i ON r.instrument_id = i.id
                INNER JOIN dam d ON i.dam_id = d.id
                WHERE d.client_id = :clientId
                  AND i.active = true
                  AND r.active = true
                GROUP BY r.instrument_id, r.date, r.hour
            )
            SELECT instrument_id, date, hour
            FROM instrument_date_hours
            WHERE date_hour_rank <= :limit
            ORDER BY instrument_id, date_hour_rank
            """, nativeQuery = true)
    List<Object[]> findLatestDistinctDateHoursByClientId(
            @Param("clientId") Long clientId,
            @Param("limit") int limit);

    // ==========================================
    // BUSCA LATEST READINGS POR INSTRUMENTOS
    // ==========================================
    @Query(value = """
            WITH ranked AS (
                SELECT
                    r.id,
                    ROW_NUMBER() OVER (
                        PARTITION BY r.instrument_id
                        ORDER BY r.date DESC, r.hour DESC
                    ) as rn
                FROM reading r
                WHERE r.instrument_id IN :instrumentIds
                  AND r.active = true
                  AND (CAST(:startDate AS date) IS NULL OR r.date >= :startDate)
                  AND (CAST(:endDate AS date) IS NULL OR r.date <= :endDate)
            )
            SELECT r.id FROM ranked r WHERE r.rn <= :limit
            """, nativeQuery = true)
    List<Long> findLatestReadingIdsByInstrumentIds(
            @Param("instrumentIds") List<Long> instrumentIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("limit") int limit);

    @Query("""
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.id IN :ids
            ORDER BY r.instrument.id, r.date DESC, r.hour DESC
            """)
    List<ReadingEntity> findByIdsWithAllRelations(@Param("ids") List<Long> ids);

    default List<ReadingEntity> findLatestReadingsByInstrumentIdsWithAllRelations(
            List<Long> instrumentIds, LocalDate startDate, LocalDate endDate, int limit) {
        List<Long> ids = findLatestReadingIdsByInstrumentIds(instrumentIds, startDate, endDate, limit);
        if (ids.isEmpty()) {
            return List.of();
        }
        return findByIdsWithAllRelations(ids);
    }

    // ==========================================
    // BUSCA PARA GRUPO DE READINGS
    // ==========================================
    @Query("""
            SELECT r FROM ReadingEntity r
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.instrument.id = :instrumentId
              AND r.date = :date
              AND r.hour = :hour
              AND r.active = true
            """)
    List<ReadingEntity> findAllReadingsInGroupWithRelations(
            @Param("instrumentId") Long instrumentId,
            @Param("date") LocalDate date,
            @Param("hour") LocalTime hour);

    // ==========================================
    // BUSCA MÍNIMA PARA BULK OPERATIONS
    // ==========================================
    @Query("""
            SELECT r FROM ReadingEntity r
            JOIN FETCH r.instrument i
            JOIN FETCH i.dam d
            JOIN FETCH d.client c
            JOIN FETCH r.output o
            WHERE r.id IN :ids
            """)
    List<ReadingEntity> findAllByIdWithMinimalData(@Param("ids") List<Long> ids);

    // ==========================================
    // BUSCA PARA EXPORT
    // ==========================================
    @Query("""
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues
            WHERE r.instrument.id = :instrumentId
              AND r.active = true
              AND (CAST(:startDate AS date) IS NULL OR r.date >= :startDate)
              AND (CAST(:endDate AS date) IS NULL OR r.date <= :endDate)
            ORDER BY r.date DESC, r.hour DESC
            """)
    List<ReadingEntity> findByInstrumentIdForExport(
            @Param("instrumentId") Long instrumentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // ==========================================
    // BUSCA DE IDS AUXILIARES
    // ==========================================
    @Query("SELECT DISTINCT o.instrument.id FROM OutputEntity o WHERE o.id IN :outputIds")
    Set<Long> findInstrumentIdsByOutputIds(@Param("outputIds") List<Long> outputIds);

    @Query("SELECT r.id FROM ReadingEntity r WHERE r.instrument.id = :instrumentId")
    List<Long> findIdsByInstrumentId(@Param("instrumentId") Long instrumentId);

    @Query("SELECT r.id FROM ReadingEntity r WHERE r.output.id = :outputId")
    List<Long> findIdsByOutputId(@Param("outputId") Long outputId);

    // ==========================================
    // BULK UPDATE
    // ==========================================
    @Modifying
    @Query("UPDATE ReadingEntity r SET r.active = :active WHERE r.id IN :ids")
    int bulkUpdateActiveStatus(@Param("ids") List<Long> ids, @Param("active") Boolean active);

    // ==========================================
    // DELETE
    // ==========================================
    @Modifying
    @Query("DELETE FROM ReadingEntity r WHERE r.instrument.id = :instrumentId")
    void deleteByInstrumentId(@Param("instrumentId") Long instrumentId);

    @Modifying
    @Query("DELETE FROM ReadingEntity r WHERE r.output.id = :outputId")
    void deleteByOutputId(@Param("outputId") Long outputId);
}
