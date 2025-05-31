package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "input")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InputEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Sigla do Input é obrigatório!")
    @Column(nullable = false)
    private String acronym;

    @NotBlank(message = "Nome do Input é obrigatório!")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Precisão do Input é obrigatório!")
    @Column(nullable = false)
    private Integer precision;

    @ManyToOne
    @JoinColumn(name = "measurement_unit_id", nullable = false)
    private MeasurementUnitEntity measurementUnit;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    @JsonIgnoreProperties("inputs")
    private InstrumentEntity instrument;
}
