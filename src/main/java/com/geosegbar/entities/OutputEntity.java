package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "output")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutputEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Sigla do Output é obrigatório!")
    @Column(nullable = false)
    private String acronym;

    @NotBlank(message = "Nome do Output é obrigatório!")
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String equation;

    @NotNull(message = "Precisão do Output é obrigatória!")
    @Column(nullable = false)
    private Integer precision;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToOne(mappedBy = "output", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("output")
    private StatisticalLimitEntity statisticalLimit;

    @OneToOne(mappedBy = "output", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("output")
    private DeterministicLimitEntity deterministicLimit;

    @ManyToOne
    @JoinColumn(name = "measurement_unit_id", nullable = false)
    private MeasurementUnitEntity measurementUnit;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    @JsonIgnoreProperties("outputs")
    private InstrumentEntity instrument;
}
