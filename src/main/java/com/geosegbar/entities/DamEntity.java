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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dam")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DamEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "O campo não pode conter números!")
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    @NotNull(message = "Latitude é obrigatório!")
    private Double latitude;

    @Column(nullable = false)
    @NotNull(message = "Longitude é obrigatório!")
    private Double longitude;

    @Column(nullable = false, unique = true)
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "O campo não pode conter números!")
    @Size(min = 3, max = 3, message = "A sigla deve ter 3 caracteres!")
    private String acronym;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private StatusEntity status;

    @ManyToMany(mappedBy = "dams", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ChecklistEntity> checklists = new HashSet<>();

    @OneToMany(mappedBy = "dam", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ChecklistResponseEntity> checklistResponses = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "dam", fetch = FetchType.LAZY)
    private Set<DamPermissionEntity> damPermissions = new HashSet<>();
}
