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
@Table(name = "constant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConstantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Sigla é obrigatório!")
    @Column(nullable = false)
    private String acronym;

    @NotBlank(message = "Nome da Constante é obrigatório!")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Precisão da Constante é obrigatório!")
    @Column(nullable = false)
    private Integer precision;

    @NotNull(message = "Valor da Constante é obrigatório!")
    @Column(nullable = false)
    private Double value;

    @ManyToOne
    @JoinColumn(name = "measurement_unit_id", nullable = false)
    private MeasurementUnitEntity measurementUnit;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    @JsonIgnoreProperties("constants")
    private InstrumentEntity instrument;
}
