package com.geosegbar.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "reservoirs", indexes = {
    @Index(name = "idx_reservoir_dam_id", columnList = "dam_id"),
    @Index(name = "idx_reservoir_level_id", columnList = "level_id"),
    @Index(name = "idx_reservoir_created_at", columnList = "created_at"),
    @Index(name = "idx_reservoir_dam_level", columnList = "dam_id, level_id")
})
public class ReservoirEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dam_id", nullable = false)
    @JsonIgnoreProperties({"reservoirs", "regulatoryDam", "documentationDam", "checklists", "checklistResponses", "damPermissions"})
    private DamEntity dam;

    @ManyToOne
    @JoinColumn(name = "level_id", nullable = false)
    @JsonIgnoreProperties("reservoirs")
    private LevelEntity level;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
