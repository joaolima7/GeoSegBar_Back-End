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
@Table(name = "security_levels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityLevelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nível de segurança é obrigatório!")
    @Column(nullable = false, unique = true)
    private String level;
    
    @JsonIgnore
    @OneToMany(mappedBy = "securityLevel", fetch = FetchType.LAZY)
    private Set<RegulatoryDamEntity> regulatoryDams = new HashSet<>();
}