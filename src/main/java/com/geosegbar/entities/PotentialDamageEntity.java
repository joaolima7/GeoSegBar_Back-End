package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "potential_damages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PotentialDamageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do dano potencial é obrigatório!")
    @Column(nullable = false, unique = true)
    private String name;
    
    @JsonIgnore
    @OneToMany(mappedBy = "potentialDamage", fetch = FetchType.LAZY)
    private Set<RegulatoryDamEntity> regulatoryDams = new HashSet<>();
}