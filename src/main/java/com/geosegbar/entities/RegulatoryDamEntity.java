package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "regulatory_dam")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegulatoryDamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "dam_id", nullable = false, unique = true)
    @JsonIgnoreProperties
    private DamEntity dam;

    @Column(name = "frame_pnsb", nullable = false)
    private Boolean framePNSB;

    @Column(name = "representative_name")
    private String representativeName;

    @Email(message = "Email do representante inválido!")
    @Column(name = "representative_email")
    private String representativeEmail;

    @Pattern(regexp = "^(\\d{10,11})?$", message = "O telefone do representante deve conter 10 ou 11 dígitos numéricos ou ser vazio!")
    @Column(name = "representative_phone")
    private String representativePhone;

    @Column(name = "technical_manager_name")
    private String technicalManagerName;

    @Email(message = "Email do responsável técnico inválido!")
    @Column(name = "technical_manager_email")
    private String technicalManagerEmail;

    @Pattern(regexp = "^(\\d{10,11})?$", message = "O telefone do responsável técnico deve conter 10 ou 11 dígitos numéricos ou ser vazio!")
    @Column(name = "technical_manager_phone")
    private String technicalManagerPhone;

    @ManyToOne
    @JoinColumn(name = "security_level_id")
    private SecurityLevelEntity securityLevel;

    @Column(name = "supervisory_body_name")
    private String supervisoryBodyName;

    @ManyToOne
    @JoinColumn(name = "risk_category_id")
    private RiskCategoryEntity riskCategory;

    @ManyToOne
    @JoinColumn(name = "potential_damage_id")
    private PotentialDamageEntity potentialDamage;

    @ManyToOne
    @JoinColumn(name = "classification_dam_id")
    private ClassificationDamEntity classificationDam;
}
