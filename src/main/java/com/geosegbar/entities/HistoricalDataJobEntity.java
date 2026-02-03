package com.geosegbar.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.geosegbar.common.enums.JobStatus;

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

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "instrument_name", length = 255)
    private String instrumentName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "checkpoint_date")
    private LocalDate checkpointDate;

    @Column(name = "total_months")
    private Integer totalMonths;

    @Column(name = "processed_months")
    private Integer processedMonths;

    /**
     * Total de readings criados com sucesso
     */
    @Column(name = "created_readings")
    private Integer createdReadings;

    @Column(name = "skipped_days")
    private Integer skippedDays;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

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

    public double getProgressPercentage() {
        if (totalMonths == null || totalMonths == 0) {
            return 0.0;
        }
        return (processedMonths * 100.0) / totalMonths;
    }

    public boolean isActive() {
        return status == JobStatus.QUEUED || status == JobStatus.PROCESSING;
    }

    public boolean isFinished() {
        return status == JobStatus.COMPLETED || status == JobStatus.FAILED;
    }
}
