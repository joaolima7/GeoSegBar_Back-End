package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
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

    @NotBlank(message = "O nome da rua é obrigatório!")
    @Column(nullable = false)
    private String street;

    @NotBlank(message = "O nome do bairro é obrigatório!")
    @Size(max = 100, message = "O nome do bairro deve ter no máximo 100 caracteres!")
    @Column(nullable = false, length = 100)
    private String neighborhood;

    @Pattern(regexp = "^[0-9]+$", message = "O número do endereço deve conter apenas números!")
    @Size(max = 10, message = "O número do endereço deve ter no máximo 10 caracteres!")
    @Column(length = 10)
    private String numberAddress;

    @NotBlank(message = "O nome da cidade é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Cidade não pode conter números!")
    @Size(max = 100, message = "O nome da cidade deve ter no máximo 100 caracteres!")
    @Column(nullable = false, length = 100)
    private String city;

    @NotBlank(message = "O nome do estado é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Estado não pode conter números!")
    @Size(max = 100, message = "O nome do estado deve ter no máximo 100 caracteres!")
    @Column(nullable = false, length = 100)
    private String state;

    @NotBlank(message = "CEP é obrigatório!")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido!")
    @Column(nullable = false, length = 9)
    private String zipCode;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private StatusEntity status;

    @Column(nullable = true)
    private String logoPath;

    @Column(nullable = true)
    private String damImagePath;

    @Column(nullable = true)
    private String linkPSB;
    
    @Column(nullable = true)
    private String linkLegislation;

    @OneToOne(mappedBy = "dam", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"dam"}) 
    private DocumentationDamEntity documentationDam;

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
