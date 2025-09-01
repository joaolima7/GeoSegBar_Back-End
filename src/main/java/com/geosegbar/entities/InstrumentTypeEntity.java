package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instrument_type",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_instrument_type_name", columnNames = "name")
        },
        indexes = {
            @Index(name = "idx_instrument_type_name_search", columnList = "name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome do tipo de instrumento é obrigatório")
    @Column(nullable = false)
    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "instrumentType")
    private Set<InstrumentEntity> instruments = new HashSet<>();
}
