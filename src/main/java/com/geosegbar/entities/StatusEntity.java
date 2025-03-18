package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.geosegbar.common.enums.StatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "status")
public class StatusEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Status é obrigatório!")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEnum status;
    
    @JsonIgnore
    @OneToMany(mappedBy = "status", fetch = FetchType.LAZY)
    private Set<UserEntity> users = new HashSet<>();
    
    @JsonIgnore
    @OneToMany(mappedBy = "status", fetch = FetchType.LAZY)
    private Set<ClientEntity> clients = new HashSet<>();
    
    @JsonIgnore
    @OneToMany(mappedBy = "status", fetch = FetchType.LAZY)
    private Set<DamEntity> dams = new HashSet<>();
}