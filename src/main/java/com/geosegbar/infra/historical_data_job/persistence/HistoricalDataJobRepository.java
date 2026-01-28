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

    /**
     * Busca job ativo (QUEUED ou PROCESSING) para um instrumento específico
     *
     * Garante que não haja múltiplos jobs ativos para o mesmo instrumento.
     *
     * @param instrumentId ID do instrumento
     * @param statuses Lista de status a buscar
     * @return Job encontrado, se existir
     */
    Optional<HistoricalDataJobEntity> findByInstrumentIdAndStatusIn(
            Long instrumentId,
            List<JobStatus> statuses
    );

    /**
     * Busca todos os jobs com status específico, ordenados por data de criação
     *
     * Usado pelo scheduler para buscar jobs QUEUED e processá-los em ordem
     * FIFO.
     *
     * @param status Status do job
     * @return Lista de jobs ordenados por criação (mais antigos primeiro)
     */
    List<HistoricalDataJobEntity> findByStatusOrderByCreatedAtAsc(JobStatus status);

    /**
     * Busca jobs que estão em PROCESSING há muito tempo (provavelmente
     * travados)
     *
     * Jobs em PROCESSING por mais de 1 hora são considerados stalled e devem
     * ser reiniciados ou marcados como FAILED.
     *
     * @param status Status PROCESSING
     * @param timeout Data/hora limite (ex: now - 1 hora)
     * @return Lista de jobs possivelmente travados
     */
    @Query("SELECT j FROM HistoricalDataJobEntity j WHERE j.status = :status "
            + "AND j.startedAt < :timeout")
    List<HistoricalDataJobEntity> findStalledJobs(
            @Param("status") JobStatus status,
            @Param("timeout") LocalDateTime timeout
    );

    /**
     * Conta quantos jobs existem com status específico
     *
     * Usado para métricas Prometheus e monitoring.
     *
     * @param status Status do job
     * @return Quantidade de jobs com esse status
     */
    Long countByStatus(JobStatus status);

    /**
     * Busca jobs de um instrumento específico
     *
     * Útil para visualizar histórico de processamento de um instrumento.
     *
     * @param instrumentId ID do instrumento
     * @return Lista de jobs do instrumento
     */
    List<HistoricalDataJobEntity> findByInstrumentIdOrderByCreatedAtDesc(Long instrumentId);

    /**
     * Busca jobs criados em um período específico
     *
     * @param startDate Data inicial
     * @param endDate Data final
     * @return Lista de jobs criados no período
     */
    List<HistoricalDataJobEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Verifica se existe algum job ativo para o instrumento
     *
     * @param instrumentId ID do instrumento
     * @return true se há job QUEUED ou PROCESSING para este instrumento
     */
    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END "
            + "FROM HistoricalDataJobEntity j "
            + "WHERE j.instrumentId = :instrumentId "
            + "AND j.status IN ('QUEUED', 'PROCESSING')")
    boolean existsActiveJobForInstrument(@Param("instrumentId") Long instrumentId);
}
