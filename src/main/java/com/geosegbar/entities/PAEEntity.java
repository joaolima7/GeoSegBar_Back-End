package com.geosegbar.entities;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pae", indexes = {
    @Index(name = "idx_pae_dam_id", columnList = "dam_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PAEEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "dam_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"pae", "sections", "reservoirs", "instruments", "psbFolders",
            "patternFolders", "templateQuestionnaires", "checklistResponses",
            "damPermissions", "regulatoryDam", "documentationDam", "checklist"})
    private DamEntity dam;

    @Column(name = "coordinator_name")
    private String coordinatorName;

    @Column(name = "coordinator_phone")
    private String coordinatorPhone;

    @Email(message = "Email do coordenador inválido!")
    @Column(name = "coordinator_email")
    private String coordinatorEmail;

    @Column(name = "substitute_coordinator_name")
    private String substituteCoordinatorName;

    @Column(name = "substitute_coordinator_phone")
    private String substituteCoordinatorPhone;

    @Email(message = "Email do coordenador substituto inválido!")
    @Column(name = "substitute_coordinator_email")
    private String substituteCoordinatorEmail;

    @Column(name = "residences")
    private Integer residences;

    @Column(name = "people")
    private Integer people;

    @Column(name = "sensible_points")
    private Integer sensiblePoints;

    @Column(name = "last_cadastral_survey")
    private LocalDate lastCadastralSurvey;

    @Column(name = "simulation_participants")
    private Integer simulationParticipants;

    @Column(name = "last_simulation_date")
    private LocalDate lastSimulationDate;

    @OneToMany(mappedBy = "pae", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("pae")
    private Set<PAEProtectionElementEntity> protectionElements = new HashSet<>();

    @OneToMany(mappedBy = "pae", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("pae")
    private Set<PAEZoneContactEntity> contacts = new HashSet<>();
}
