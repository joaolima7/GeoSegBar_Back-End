package com.geosegbar.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.geosegbar.common.enums.WeatherConditionEnum;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "checklist_responses", indexes = {
    @Index(name = "idx_checklist_response_dam_id", columnList = "dam_id"),
    @Index(name = "idx_checklist_response_user_id", columnList = "user_id"),
    @Index(name = "idx_checklist_response_checklist_id", columnList = "checklist_id"),
    @Index(name = "idx_checklist_response_created_at", columnList = "created_at"),
    @Index(name = "idx_checklist_response_dam_created_desc", columnList = "dam_id, created_at"),
    @Index(name = "idx_checklist_response_dam_checklist_created", columnList = "dam_id, checklist_id, created_at"),
    @Index(name = "idx_checklist_response_dam_period", columnList = "dam_id, created_at"),
    @Index(name = "idx_checklist_response_user_created", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChecklistResponseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome do checklist é obrigatório!")
    @Column(name = "checklist_name", nullable = false)
    private String checklistName;

    @NotNull(message = "ID do checklist é obrigatório!")
    @Column(name = "checklist_id", nullable = false)
    private Long checklistId;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @NotNull(message = "Informe a barragem que corresponde a essa resposta de checklist!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dam_id", nullable = false)
    private DamEntity dam;

    @NotNull(message = "Informe o usuário que respondeu esse checklist!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(mappedBy = "checklistResponse", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "checklist-questionnaire-responses")
    private Set<QuestionnaireResponseEntity> questionnaireResponses = new HashSet<>();

    @Column(name = "upstream_level")
    private Double upstreamLevel;

    @Column(name = "downstream_level")
    private Double downstreamLevel;

    @Column(name = "spilled_flow")
    private Double spilledFlow;

    @Column(name = "turbined_flow")
    private Double turbinedFlow;

    @Column(name = "accumulated_rainfall")
    private Double accumulatedRainfall;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_condition", length = 50)
    private WeatherConditionEnum weatherCondition;
}
