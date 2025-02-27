package com.geosegbar.infra.dam.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dam")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DamModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório!")
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    @NotNull(message = "Latitude é obrigatório!")
    private Double latitude;

    @Column(nullable = false)
    @NotNull(message = "Longitude é obrigatório!")
    private Double longitude;

    @Column(nullable = false, unique = true)
    @Size(min = 3, max = 3, message = "A sigla deve ter 3 caracteres!")
    private String acronym;
}
