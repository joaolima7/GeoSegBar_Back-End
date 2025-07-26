package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome da Seção é obrigatório!")
    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String filePath;

    @NotNull(message = "Primeiro vértice da Latitude é obrigatório!")
    @Column(nullable = false)
    private Double firstVertexLatitude;

    @NotNull(message = "Segundo vértice da Latitude é obrigatório!")
    @Column(nullable = false)
    private Double secondVertexLatitude;

    @NotNull(message = "Primeiro vértice da Longitude é obrigatório!")
    @Column(nullable = false)
    private Double firstVertexLongitude;

    @NotNull(message = "Segundo vértice da Longitude é obrigatório!")
    @Column(nullable = false)
    private Double secondVertexLongitude;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "dam_id", nullable = true)
    @JsonIgnoreProperties("sections")
    private DamEntity dam;

    @JsonIgnore
    @OneToMany(mappedBy = "section")
    private Set<InstrumentEntity> instruments = new HashSet<>();
}
