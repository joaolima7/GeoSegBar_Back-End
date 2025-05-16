package com.geosegbar.entities;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.geosegbar.common.enums.AnomalyOriginEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "anomalies")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AnomalyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"damPermissions", "createdUsers", "psbFoldersCreated", "psbFilesUploaded", "sharedFolders"})
    @NotNull(message = "Usuário é obrigatório!")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dam_id", nullable = false)
    @JsonIgnoreProperties({"checklists", "checklistResponses", "damPermissions", "reservoirs", "psbFolders"})
    @NotNull(message = "Barragem é obrigatória!")
    private DamEntity dam;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @NotNull(message = "Latitude é obrigatória!")
    private Double latitude;

    @Column(nullable = false)
    @NotNull(message = "Longitude é obrigatória!")
    private Double longitude;

    @Column(name = "questionnaire_id")
    private Long questionnaireId;

    @Column(name = "question_id")
    private Long questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false)
    private AnomalyOriginEnum origin;

    @Column(name = "observation", columnDefinition = "TEXT")
    private String observation;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "photo_path", nullable = false)
    private String photoPath;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "danger_level_id", nullable = false)
    @NotNull(message = "Nível de Perigo é obrigatório!")
    private DangerLevelEntity dangerLevel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_id", nullable = false)
    @NotNull(message = "Status é obrigatório!")
    private AnomalyStatusEntity status;
}
