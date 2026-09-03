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
import com.geosegbar.infra.dashboard.projections.CategoryCountProjection;
import com.geosegbar.infra.dashboard.projections.InstrumentStatusDistributionProjection;
import com.geosegbar.infra.dashboard.projections.InstrumentTypeCountProjection;
import com.geosegbar.infra.mobile_dashboard.projections.CriticalInstrumentProjection;
import com.geosegbar.infra.mobile_dashboard.projections.MonthlyCountProjection;
import com.geosegbar.infra.reading.projections.InstrumentLimitStatusProjection;

@Repository
public interface ReadingRepository extends JpaRepository<ReadingEntity, Long> {

    @Query(value = """
            WITH latest_per_instrument AS (
                SELECT DISTINCT ON (r.instrument_id)
                    r.instrument_id, r.date, r.hour
                FROM reading r
                INNER JOIN instrument i ON r.instrument_id = i.id
                WHERE r.active = true
                  AND i.active = true
                  AND i.dam_id IN (:damIds)
                  AND r.date >= :startDate
                  AND r.date <= :endDate
                ORDER BY r.instrument_id, r.date DESC, r.hour DESC
            ),
            critical_status AS (
                SELECT DISTINCT ON (r.instrument_id)
                    r.instrument_id,
                    r.limit_status
                FROM reading r
                INNER JOIN latest_per_instrument lpi
                    ON r.instrument_id = lpi.instrument_id
                    AND r.date = lpi.date
                    AND r.hour = lpi.hour
                WHERE r.active = true
                ORDER BY r.instrument_id,
                    CASE r.limit_status
                        WHEN 'EMERGENCIA' THEN 1
                        WHEN 'ALERTA' THEN 2
                        WHEN 'ATENCAO' THEN 3
                        WHEN 'SUPERIOR' THEN 4
                        WHEN 'INFERIOR' THEN 4
                        WHEN 'NORMAL' THEN 5
                    END,
                    r.limit_status
            )
            SELECT
                it.id as typeId,
                it.name as typeName,
                cs.limit_status as limitStatus,
                CAST(COUNT(*) AS BIGINT) as total
            FROM critical_status cs
            INNER JOIN instrument i ON cs.instrument_id = i.id
            INNER JOIN instrument_type it ON i.instrument_type_id = it.id
            GROUP BY it.id, it.name, cs.limit_status
            ORDER BY it.name
            """, nativeQuery = true)
    List<InstrumentStatusDistributionProjection> findInstrumentStatusDistributionByType(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByUser_Id(Long userId);

    boolean existsByInstrumentId(Long instrumentId);

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

    @Query(value = """
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
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

    @Query(value = """
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH i.instrumentType
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
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

    @Query(value = """
            SELECT r.date, r.hour
            FROM ReadingEntity r
            WHERE r.instrument.id = :instrumentId
              AND (:active IS NULL OR r.active = :active)
            GROUP BY r.date, r.hour
            ORDER BY r.date DESC, r.hour DESC
            """,
            countQuery = """
                SELECT COUNT(DISTINCT CONCAT(CAST(r.date AS String), ' ', CAST(r.hour AS String)))
                FROM ReadingEntity r
                WHERE r.instrument.id = :instrumentId
                  AND (:active IS NULL OR r.active = :active)
                """)
    Page<Object[]> findDistinctDateHourByInstrumentIdAndActive(
            @Param("instrumentId") Long instrumentId,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM ReadingEntity r
            JOIN FETCH r.output o
            LEFT JOIN FETCH r.user u
            LEFT JOIN FETCH r.inputValues iv
            WHERE r.instrument.id = :instrumentId
              AND r.date IN :dates
              AND r.hour IN :hours
              AND (:active IS NULL OR r.active = :active)
            ORDER BY r.date DESC, r.hour DESC, o.name ASC
            """)
    List<ReadingEntity> findByInstrumentIdAndDatesOptimized(
            @Param("instrumentId") Long instrumentId,
            @Param("dates") List<LocalDate> dates,
            @Param("hours") List<LocalTime> hours,
            @Param("active") Boolean active);

    @Query(value = """
            SELECT DISTINCT r.date, r.hour
            FROM ReadingEntity r
            WHERE r.instrument.id IN :instrumentIds AND r.active = true
            ORDER BY r.date DESC, r.hour DESC
            """,
            countQuery = """
                SELECT COUNT(DISTINCT CONCAT(CAST(r.date AS String), '_', CAST(r.hour AS String)))
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

    @Query(value = """
            SELECT r.id
            FROM reading r
            WHERE r.instrument_id IN :instrumentIds
              AND r.active = true
            ORDER BY r.instrument_id, r.date DESC, r.hour DESC
            """, nativeQuery = true)
    List<Long> findAllReadingIdsByInstrumentIds(@Param("instrumentIds") List<Long> instrumentIds);

    default List<ReadingEntity> findLatestReadingsByInstrumentIdsWithAllRelations(
            List<Long> instrumentIds, LocalDate startDate, LocalDate endDate, int limit) {
        List<Long> ids;
        if (startDate == null && endDate == null) {
            ids = findAllReadingIdsByInstrumentIds(instrumentIds);
        } else {
            ids = findLatestReadingIdsByInstrumentIds(instrumentIds, startDate, endDate, limit);
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return findByIdsWithAllRelations(ids);
    }

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

    @Query("""
            SELECT r FROM ReadingEntity r
            JOIN FETCH r.instrument i
            JOIN FETCH i.dam d
            JOIN FETCH d.client c
            JOIN FETCH r.output o
            WHERE r.id IN :ids
            """)
    List<ReadingEntity> findAllByIdWithMinimalData(@Param("ids") List<Long> ids);

    @Query("""
        SELECT DISTINCT r FROM ReadingEntity r
        LEFT JOIN FETCH r.instrument i
        LEFT JOIN FETCH i.instrumentType
        LEFT JOIN FETCH r.output o
        LEFT JOIN FETCH r.user u
        LEFT JOIN FETCH r.inputValues
        WHERE r.instrument.id = :instrumentId
          AND r.date IN :dates
          AND (:active IS NULL OR r.active = :active)
        ORDER BY r.date DESC, r.hour DESC
        """)
    List<ReadingEntity> findByInstrumentIdAndDatesWithAllRelations(
            @Param("instrumentId") Long instrumentId,
            @Param("dates") List<LocalDate> dates,
            @Param("active") Boolean active);

    @Query("""
            SELECT DISTINCT r FROM ReadingEntity r
            LEFT JOIN FETCH r.instrument i
            LEFT JOIN FETCH r.output o
            LEFT JOIN FETCH o.measurementUnit mu  
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

    @Query("SELECT DISTINCT o.instrument.id FROM OutputEntity o WHERE o.id IN :outputIds")
    Set<Long> findInstrumentIdsByOutputIds(@Param("outputIds") List<Long> outputIds);

    @Query("SELECT r.id FROM ReadingEntity r WHERE r.instrument.id = :instrumentId")
    List<Long> findIdsByInstrumentId(@Param("instrumentId") Long instrumentId);

    @Query("SELECT r.id FROM ReadingEntity r WHERE r.output.id = :outputId")
    List<Long> findIdsByOutputId(@Param("outputId") Long outputId);

    @Modifying
    @Query("UPDATE ReadingEntity r SET r.active = :active WHERE r.id IN :ids")
    int bulkUpdateActiveStatus(@Param("ids") List<Long> ids, @Param("active") Boolean active);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ReadingEntity r SET r.active = :active WHERE r.id IN :ids")
    int updateActiveStatusByIds(@Param("active") Boolean active, @Param("ids") List<Long> ids);

    @Modifying
    @Query("DELETE FROM ReadingEntity r WHERE r.instrument.id = :instrumentId")
    void deleteByInstrumentId(@Param("instrumentId") Long instrumentId);

    @Modifying
    @Query("DELETE FROM ReadingEntity r WHERE r.output.id = :outputId")
    void deleteByOutputId(@Param("outputId") Long outputId);

    // Returns one row per output_id: the single most-recent active reading for each output in the list.
    // Uses a CTE with ROW_NUMBER so the DB executes a single pass instead of N correlated subqueries.
    @Query(value = """
            WITH ranked AS (
                SELECT r.id,
                       r.output_id,
                       ROW_NUMBER() OVER (PARTITION BY r.output_id ORDER BY r.date DESC, r.hour DESC) AS rn
                FROM reading r
                WHERE r.output_id IN :outputIds
                  AND r.active = true
            )
            SELECT r.id FROM ranked r WHERE r.rn = 1
            """, nativeQuery = true)
    List<Long> findLatestReadingIdsByOutputIds(@Param("outputIds") List<Long> outputIds);

    // ===================== Painel do aplicativo (/mobile/dashboard) =====================
    //
    // Um cuidado que atravessa as quatro consultas abaixo: a tabela reading
    // guarda UMA LINHA POR SAÍDA (output) do instrumento. Um piezômetro com
    // três saídas grava três linhas para a mesma visita. Contar linhas
    // responderia "quantos valores foram calculados", não "quantas leituras o
    // inspetor fez" — que é a pergunta do painel. Por isso
    // COUNT(DISTINCT (instrument_id, date, hour)): a visita é a chave.
    /**
     * Quantas leituras ESTE usuário registrou, mês a mês, nas barragens que
     * ele acessa. No máximo 12 linhas de resposta.
     */
    @Query(value = """
            SELECT to_char(r.date, 'YYYY-MM') AS bucket,
                   CAST(COUNT(DISTINCT (r.instrument_id, r.date, r.hour)) AS BIGINT) AS total
            FROM reading r
            INNER JOIN instrument i ON i.id = r.instrument_id
            WHERE r.user_id = :userId
              AND r.active = true
              AND i.dam_id IN (:damIds)
              AND r.date >= :startDate
              AND r.date <= :endDate
            GROUP BY to_char(r.date, 'YYYY-MM')
            ORDER BY 1 ASC
            """, nativeQuery = true)
    List<MonthlyCountProjection> countMyReadingsByMonth(
            @Param("userId") Long userId,
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Onde o esforço de leitura deste usuário foi parar, por tipo de
     * instrumento. Uma linha por tipo.
     */
    @Query(value = """
            SELECT it.id AS typeId,
                   it.name AS typeName,
                   CAST(COUNT(DISTINCT (r.instrument_id, r.date, r.hour)) AS BIGINT) AS total
            FROM reading r
            INNER JOIN instrument i ON i.id = r.instrument_id
            INNER JOIN instrument_type it ON it.id = i.instrument_type_id
            WHERE r.user_id = :userId
              AND r.active = true
              AND i.dam_id IN (:damIds)
              AND r.date >= :startDate
              AND r.date <= :endDate
            GROUP BY it.id, it.name
            ORDER BY it.name ASC
            """, nativeQuery = true)
    List<InstrumentTypeCountProjection> countMyReadingsByInstrumentType(
            @Param("userId") Long userId,
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Como estão os instrumentos AGORA: quantos em cada estado de limite,
     * olhando a última leitura de cada um.
     *
     * Duas etapas, as duas resolvidas por DISTINCT ON sobre
     * idx_reading_instrument_active_date_hour:
     *
     * 1. latest_per_instrument acha a visita mais recente de cada instrumento;
     * 2. worst_status escolhe, ENTRE AS SAÍDAS daquela mesma visita, a de pior
     *    estado. Sem isso um piezômetro com uma saída em EMERGENCIA e outra em
     *    NORMAL poderia ser contado como normal, dependendo da ordem física das
     *    linhas — e num app de segurança de barragem esse é o erro que não se
     *    pode cometer. A ordem de gravidade é a mesma já usada pelo painel da
     *    web, para os dois números não divergirem.
     *
     * Instrumento sem leitura no período simplesmente não aparece; quem fecha a
     * conta com o total é o serviço, que expõe instrumentsWithoutReading.
     */
    @Query(value = """
            WITH latest_per_instrument AS (
                SELECT DISTINCT ON (r.instrument_id)
                    r.instrument_id, r.date, r.hour
                FROM reading r
                INNER JOIN instrument i ON i.id = r.instrument_id
                WHERE r.active = true
                  AND i.active = true
                  AND i.dam_id IN (:damIds)
                  AND r.date >= :startDate
                  AND r.date <= :endDate
                ORDER BY r.instrument_id, r.date DESC, r.hour DESC
            ),
            worst_status AS (
                SELECT DISTINCT ON (r.instrument_id)
                    r.instrument_id, r.limit_status
                FROM reading r
                INNER JOIN latest_per_instrument lpi
                    ON r.instrument_id = lpi.instrument_id
                    AND r.date = lpi.date
                    AND r.hour = lpi.hour
                WHERE r.active = true
                ORDER BY r.instrument_id,
                    CASE r.limit_status
                        WHEN 'EMERGENCIA' THEN 1
                        WHEN 'ALERTA' THEN 2
                        WHEN 'ATENCAO' THEN 3
                        WHEN 'SUPERIOR' THEN 4
                        WHEN 'INFERIOR' THEN 4
                        ELSE 5
                    END
            )
            SELECT w.limit_status AS name,
                   CAST(COUNT(*) AS BIGINT) AS count
            FROM worst_status w
            GROUP BY w.limit_status
            """, nativeQuery = true)
    List<CategoryCountProjection> countInstrumentsByLimitStatus(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Os instrumentos fora do normal, do mais grave para o menos, já com o nome
     * da barragem. É o que o painel mostra como "precisa de atenção agora" e o
     * que dá o toque único até o instrumento.
     *
     * Mesmas duas etapas da consulta acima; aqui a lista sai limitada, para o
     * aparelho nunca receber uma lista longa que ele não vai desenhar.
     */
    @Query(value = """
            WITH latest_per_instrument AS (
                SELECT DISTINCT ON (r.instrument_id)
                    r.instrument_id, r.date, r.hour
                FROM reading r
                INNER JOIN instrument i ON i.id = r.instrument_id
                WHERE r.active = true
                  AND i.active = true
                  AND i.dam_id IN (:damIds)
                  AND r.date >= :startDate
                  AND r.date <= :endDate
                ORDER BY r.instrument_id, r.date DESC, r.hour DESC
            ),
            worst_status AS (
                SELECT DISTINCT ON (r.instrument_id)
                    r.instrument_id, r.limit_status, r.date
                FROM reading r
                INNER JOIN latest_per_instrument lpi
                    ON r.instrument_id = lpi.instrument_id
                    AND r.date = lpi.date
                    AND r.hour = lpi.hour
                WHERE r.active = true
                ORDER BY r.instrument_id,
                    CASE r.limit_status
                        WHEN 'EMERGENCIA' THEN 1
                        WHEN 'ALERTA' THEN 2
                        WHEN 'ATENCAO' THEN 3
                        WHEN 'SUPERIOR' THEN 4
                        WHEN 'INFERIOR' THEN 4
                        ELSE 5
                    END
            )
            SELECT i.id AS instrumentId,
                   i.name AS instrumentName,
                   d.id AS damId,
                   d.name AS damName,
                   w.limit_status AS limitStatus,
                   w.date AS lastReadingDate
            FROM worst_status w
            INNER JOIN instrument i ON i.id = w.instrument_id
            INNER JOIN dam d ON d.id = i.dam_id
            WHERE w.limit_status IN ('EMERGENCIA', 'ALERTA', 'ATENCAO')
            ORDER BY CASE w.limit_status
                        WHEN 'EMERGENCIA' THEN 1
                        WHEN 'ALERTA' THEN 2
                        ELSE 3
                     END,
                     w.date DESC,
                     i.name ASC
            LIMIT :maxItems
            """, nativeQuery = true)
    List<CriticalInstrumentProjection> findCriticalInstruments(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("maxItems") int maxItems);
}
