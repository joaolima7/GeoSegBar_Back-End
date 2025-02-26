package com.geosegbar.infra.dam.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dam")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DamModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Longitude é obrigatório!")
    @Column(nullable = false)
    private String name;

    @Column(precision = 9, scale = 6, nullable = false)
    @NotBlank(message = "Latitude é obrigatório!")
    private Double latitude;

    @Column(precision = 9, scale = 6, nullable = false)
    @NotBlank(message = "Longitude é obrigatório!")
    private Double longitude;

    @Column(nullable = false)
    @Size(min = 3, max = 3, message = "A sigla deve ter 3 caracteres!")
    private String acronym;
}
