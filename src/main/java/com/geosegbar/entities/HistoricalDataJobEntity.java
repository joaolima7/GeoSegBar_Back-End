package com.geosegbar.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.geosegbar.common.enums.JobStatus;

/**
 * Entidade que representa um job de coleta de dados históricos
 *
 * Armazena o estado persistente de um job de coleta de 10 anos de dados
 * hidrotelemetricos para um instrumento régua linimétrica.
 *
 * O job é executado em background, coletando dados da API da ANA em lotes
 * mensais (30 dias) e inserindo no banco em batches de 30-50 registros.
 */
@Entity
@Table(name = "historical_data_job", indexes = {
    @Index(name = "idx_job_instrument_status", columnList = "instrument_id, status"),
    @Index(name = "idx_job_status", columnList = "status"),
    @Index(name = "idx_job_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalDataJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID do instrumento (régua linimétrica) para o qual os dados serão
     * coletados
     */
    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    /**
     * Nome do instrumento (para facilitar logs e monitoring)
     */
    @Column(name = "instrument_name", length = 255)
    private String instrumentName;

    /**
     * Status atual do job
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    /**
     * Data de início do período a ser coletado (geralmente 10 anos atrás)
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Data final do período a ser coletado (geralmente hoje)
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Data atual do processamento - checkpoint para resume em caso de falha
     * Permite retomar de onde parou sem reprocessar tudo
     */
    @Column(name = "checkpoint_date")
    private LocalDate checkpointDate;

    /**
     * Total de meses a processar (calculado na criação do job)
     */
    @Column(name = "total_months")
    private Integer totalMonths;

    /**
     * Meses já processados com sucesso
     */
    @Column(name = "processed_months")
    private Integer processedMonths;

    /**
     * Total de readings criados com sucesso
     */
    @Column(name = "created_readings")
    private Integer createdReadings;

    /**
     * Dias que foram pulados (sem dados ou duplicados)
     */
    @Column(name = "skipped_days")
    private Integer skippedDays;

    /**
     * Contador de tentativas de retry (máximo 3)
     */
    @Column(name = "retry_count")
    private Integer retryCount;

    /**
     * Mensagem do último erro (se houver)
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /**
     * Data/hora de criação do job
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data/hora em que o processamento iniciou
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * Data/hora em que o job foi completado ou falhou
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = JobStatus.QUEUED;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (processedMonths == null) {
            processedMonths = 0;
        }
        if (createdReadings == null) {
            createdReadings = 0;
        }
        if (skippedDays == null) {
            skippedDays = 0;
        }
    }

    /**
     * Calcula o progresso do job em porcentagem
     *
     * @return Progresso de 0 a 100
     */
    public double getProgressPercentage() {
        if (totalMonths == null || totalMonths == 0) {
            return 0.0;
        }
        return (processedMonths * 100.0) / totalMonths;
    }

    /**
     * Verifica se o job está em estado ativo (pode estar sendo processado)
     *
     * @return true se status é QUEUED ou PROCESSING
     */
    public boolean isActive() {
        return status == JobStatus.QUEUED || status == JobStatus.PROCESSING;
    }

    /**
     * Verifica se o job está em estado final (completado ou falhou
     * definitivamente)
     *
     * @return true se status é COMPLETED ou FAILED
     */
    public boolean isFinished() {
        return status == JobStatus.COMPLETED || status == JobStatus.FAILED;
    }
}
