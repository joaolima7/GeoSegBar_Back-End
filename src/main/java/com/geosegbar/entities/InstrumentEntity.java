package com.geosegbar.entities;

import java.time.LocalDateTime;
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
@Table(name = "instrument", indexes = {
    @Index(name = "idx_instrument_dam_active", columnList = "dam_id, active"),
    @Index(name = "idx_instrument_section_active", columnList = "section_id, active"),
    @Index(name = "idx_instrument_name", columnList = "name"),
    @Index(name = "idx_instrument_active_section", columnList = "active, activeForSection"),
    @Index(name = "idx_instrument_type_active", columnList = "instrument_type_id, active"),
    @Index(name = "idx_instrument_dam_type", columnList = "dam_id, instrument_type_id"),
    @Index(name = "idx_instrument_coordinates", columnList = "latitude, longitude"),
    @Index(name = "idx_instrument_dam_coordinates", columnList = "dam_id, latitude, longitude"),
    @Index(name = "idx_instrument_dam_section", columnList = "dam_id, section_id"),
    @Index(name = "idx_instrument_linimetric", columnList = "is_linimetric_ruler")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstrumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome do Instrumento é obrigatório!")
    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String location;

    @Column(nullable = true)
    private Double distanceOffset;

    @Column(nullable = true)
    private Double latitude;

    @Column(nullable = true)
    private Double longitude;

    @NotNull(message = "Campo 'Sem limites' do Instrumento é obrigatório!")
    @Column(nullable = false)
    private Boolean noLimit;

    @ManyToOne
    @JoinColumn(name = "dam_id", nullable = false)
    @JsonIgnoreProperties({"instruments"})
    private DamEntity dam;

    @ManyToOne
    @JoinColumn(name = "instrument_type_id", nullable = false)
    private InstrumentTypeEntity instrumentType;

    @NotNull(message = "Campo 'Ativo' do Instrumento é obrigatório!")
    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean activeForSection = true;

    @Column(nullable = false)
    private LocalDateTime lastUpdateVariablesDate;

    @Column(name = "is_linimetric_ruler", nullable = false)
    private Boolean isLinimetricRuler;

    @Column(name = "linimetric_ruler_code", nullable = true)
    private Long linimetricRulerCode;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = true)
    private SectionEntity section;

    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("instrument")
    private Set<InputEntity> inputs = new HashSet<>();

    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("instrument")
    private Set<ConstantEntity> constants = new HashSet<>();

    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("instrument")
    private Set<OutputEntity> outputs = new HashSet<>();

    @OneToMany(mappedBy = "instrument", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("instrument")
    private Set<ReadingEntity> readings = new HashSet<>();
}
