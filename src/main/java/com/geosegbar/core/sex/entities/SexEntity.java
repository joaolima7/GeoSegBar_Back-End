package com.geosegbar.core.sex.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.geosegbar.core.user.entities.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sex")
public class SexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome do Sexo é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "O campo não pode conter números!")
    @Column(nullable = false, unique = true)
    private String name;

    @JsonProperty(access = Access.WRITE_ONLY)
    @OneToMany(mappedBy = "sex")
    private Set<UserEntity> users = new HashSet<>();
}
