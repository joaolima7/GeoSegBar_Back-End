package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instrument_graph_pattern", indexes = {
    @Index(name = "idx_graph_pattern_instrument", columnList = "instrument_id"),
    @Index(name = "idx_graph_pattern_name", columnList = "name"),
    @Index(name = "idx_graph_pattern_folder", columnList = "folder_id"),
    @Index(name = "idx_graph_pattern_instrument_name", columnList = "instrument_id, name"),
    @Index(name = "idx_graph_pattern_folder_name", columnList = "folder_id, name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstrumentGraphPatternEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome do padrão é obrigatório!")
    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    @JsonIgnoreProperties({"readings", "inputs", "constants", "outputs"})
    private InstrumentEntity instrument;

    @ManyToOne
    @JoinColumn(name = "folder_id", nullable = true)
    @JsonIgnoreProperties({"patterns"})
    private InstrumentGraphPatternFolder folder;

    @OneToMany(mappedBy = "pattern", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties("pattern")
    private Set<InstrumentGraphCustomizationPropertiesEntity> properties = new HashSet<>();

    @OneToOne(mappedBy = "pattern", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties("pattern")
    private InstrumentGraphAxesEntity axes;
}
