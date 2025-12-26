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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dam", indexes = {
    @Index(name = "idx_dam_client_id", columnList = "client_id"),
    @Index(name = "idx_dam_status_id", columnList = "status_id"),
    @Index(name = "idx_dam_name", columnList = "name"),
    @Index(name = "idx_dam_coords", columnList = "latitude, longitude"),
    @Index(name = "idx_dam_city_state", columnList = "city, state"),
    @Index(name = "idx_dam_zip", columnList = "zip_code"),
    @Index(name = "idx_dam_latitude", columnList = "latitude"),
    @Index(name = "idx_dam_longitude", columnList = "longitude"),
    @Index(name = "idx_dam_city", columnList = "city"),
    @Index(name = "idx_dam_state", columnList = "state"),
    @Index(name = "idx_dam_client_status", columnList = "client_id, status_id"),
    @Index(name = "idx_dam_client_name", columnList = "client_id, name"),
    @Index(name = "idx_dam_status_city", columnList = "status_id, city"),
    @Index(name = "idx_dam_client_coords", columnList = "client_id, latitude, longitude"),
    @Index(name = "idx_dam_client_geo_status", columnList = "client_id, status_id, latitude, longitude")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DamEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "O campo não pode conter números!")
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @NotNull(message = "Latitude é obrigatório!")
    private Double latitude;

    @Column(nullable = false)
    @NotNull(message = "Longitude é obrigatório!")
    private Double longitude;

    @NotBlank(message = "O nome da rua é obrigatório!")
    @Column(nullable = false)
    private String street;

    @Size(max = 100, message = "O nome do bairro deve ter no máximo 100 caracteres!")
    @Column(length = 100)
    private String neighborhood;

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
    private RegulatoryDamEntity regulatoryDam;

    @OneToOne(mappedBy = "dam", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"dam"})
    private DocumentationDamEntity documentationDam;

    @OneToOne(mappedBy = "dam", fetch = FetchType.LAZY)
    @JsonIgnore
    private ChecklistEntity checklist;

    @OneToMany(mappedBy = "dam", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ChecklistResponseEntity> checklistResponses = new HashSet<>();

    @OneToMany(mappedBy = "dam", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("dam")
    private Set<SectionEntity> sections = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "dam", fetch = FetchType.LAZY)
    private Set<DamPermissionEntity> damPermissions = new HashSet<>();

    @OneToMany(mappedBy = "dam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("dam")
    private Set<ReservoirEntity> reservoirs = new HashSet<>();

    @OneToMany(mappedBy = "dam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("dam")
    private Set<PSBFolderEntity> psbFolders = new HashSet<>();

    @OneToMany(mappedBy = "dam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("dam")
    private Set<InstrumentEntity> instruments = new HashSet<>();

    @OneToMany(mappedBy = "dam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("dam")
    private Set<InstrumentGraphPatternFolder> patternFolders = new HashSet<>();

    @OneToMany(mappedBy = "dam", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<TemplateQuestionnaireEntity> templateQuestionnaires = new HashSet<>();
}
