package com.geosegbar.entities;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "documentation_dam", indexes = {
    @Index(name = "idx_doc_dam_dam_id", columnList = "dam_id", unique = true),
    @Index(name = "idx_doc_dam_next_pae", columnList = "next_update_pae"),
    @Index(name = "idx_doc_dam_next_psb", columnList = "next_update_psb"),
    @Index(name = "idx_doc_dam_next_checklist", columnList = "next_achievement_checklist")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DocumentationDamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "dam_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"documentationDam"})
    private DamEntity dam;

    @Column(name = "last_update_pae")
    private LocalDate lastUpdatePAE;

    @Column(name = "next_update_pae")
    private LocalDate nextUpdatePAE;

    @Column(name = "last_update_psb")
    private LocalDate lastUpdatePSB;

    @Column(name = "next_update_psb")
    private LocalDate nextUpdatePSB;

    @Column(name = "last_update_rpsb")
    private LocalDate lastUpdateRPSB;

    @Column(name = "next_update_rpsb")
    private LocalDate nextUpdateRPSB;

    @Column(name = "last_achievement_isr")
    private LocalDate lastAchievementISR;

    @Column(name = "next_achievement_isr")
    private LocalDate nextAchievementISR;

    @Column(name = "last_achievement_checklist")
    private LocalDate lastAchievementChecklist;

    @Column(name = "next_achievement_checklist")
    private LocalDate nextAchievementChecklist;

    @Column(name = "last_filling_fsb")
    private LocalDate lastFillingFSB;

    @Column(name = "next_filling_fsb")
    private LocalDate nextFillingFSB;

    @Column(name = "last_internal_simulation")
    private LocalDate lastInternalSimulation;

    @Column(name = "next_internal_simulation")
    private LocalDate nextInternalSimulation;

    @Column(name = "last_external_simulation")
    private LocalDate lastExternalSimulation;

    @Column(name = "next_external_simulation")
    private LocalDate nextExternalSimulation;
}
