package com.geosegbar.entities;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instrument")
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

    @NotNull(message = "Latitude do Instrumento é obrigatório!")
    @Column(nullable = false)
    private Double latitude;

    @NotNull(message = "Longitude do Instrumento é obrigatório!")
    @Column(nullable = false)
    private Double longitude;

    @NotNull(message = "Campo 'Sem limites' do Instrumento é obrigatório!")
    @Column(nullable = false)
    private Boolean noLimit;

    @ManyToOne
    @JoinColumn(name = "dam_id", nullable = false)
    @JsonIgnoreProperties({"instruments"})
    private DamEntity dam;

    @NotBlank(message = "Tipo de Instrumento é obrigatório!")
    @Column(name = "instrument_type", nullable = true)
    private String instrumentType;

    @NotNull(message = "Campo 'Ativo' do Instrumento é obrigatório!")
    @Column(nullable = false)
    private Boolean active;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private SectionEntity section;

    @OneToOne(mappedBy = "instrument", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("instrument")
    private StatisticalLimitEntity statisticalLimit;

    @OneToOne(mappedBy = "instrument", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("instrument")
    private DeterministicLimitEntity deterministicLimit;

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
