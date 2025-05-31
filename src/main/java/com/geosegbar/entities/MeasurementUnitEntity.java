package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "measurement_unit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementUnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome da Unidade de Medida é obrigatório!")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Sigla da Unidade de Medida é obrigatório!")
    @Column(nullable = false, unique = true)
    private String acronym;

    @JsonIgnore
    @OneToMany(mappedBy = "measurementUnit")
    private Set<InputEntity> inputs = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "measurementUnit")
    private Set<ConstantEntity> constants = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "measurementUnit")
    private Set<OutputEntity> outputs = new HashSet<>();
}
