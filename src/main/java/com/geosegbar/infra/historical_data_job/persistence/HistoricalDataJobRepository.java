package com.geosegbar.infra.historical_data_job.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.JobStatus;
import com.geosegbar.entities.HistoricalDataJobEntity;

/**
 * Repositório para operações de persistência de jobs de dados históricos
 */
@Repository
public interface HistoricalDataJobRepository extends JpaRepository<HistoricalDataJobEntity, Long> {

    Optional<HistoricalDataJobEntity> findByInstrumentIdAndStatusIn(
            Long instrumentId,
            List<JobStatus> statuses
    );

    List<HistoricalDataJobEntity> findByStatusOrderByCreatedAtAsc(JobStatus status);

    @Query("SELECT j FROM HistoricalDataJobEntity j WHERE j.status = :status "
            + "AND j.startedAt < :timeout")
    List<HistoricalDataJobEntity> findStalledJobs(
            @Param("status") JobStatus status,
            @Param("timeout") LocalDateTime timeout
    );

    Long countByStatus(JobStatus status);

    List<HistoricalDataJobEntity> findByInstrumentIdOrderByCreatedAtDesc(Long instrumentId);

    List<HistoricalDataJobEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END "
            + "FROM HistoricalDataJobEntity j "
            + "WHERE j.instrumentId = :instrumentId "
            + "AND j.status IN ('QUEUED', 'PROCESSING')")
    boolean existsActiveJobForInstrument(@Param("instrumentId") Long instrumentId);
}
