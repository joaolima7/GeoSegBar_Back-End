package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instrument_tabulate_pattern_folder", indexes = {
    @Index(name = "idx_tabulate_folder_name", columnList = "name"),
    @Index(name = "idx_tabulate_folder_dam", columnList = "dam_id"),
    @Index(name = "idx_tabulate_folder_dam_name", columnList = "dam_id, name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstrumentTabulatePatternFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome da pasta é obrigatório!")
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dam_id", nullable = false)
    @NotNull(message = "Barragem é obrigatória!")
    @JsonIgnoreProperties({"sections", "instruments", "hydrotelemetricReadings", "reservoirs", "psbFolders", "checklists", "checklistResponses", "damPermissions", "regulatoryDam", "documentationDam"})
    private DamEntity dam;

    @OneToMany(mappedBy = "folder", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"folder", "associations"})
    private Set<InstrumentTabulatePatternEntity> patterns = new HashSet<>();
}
