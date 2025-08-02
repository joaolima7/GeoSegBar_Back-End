package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instrument_graph_customization_properties", indexes = {
    @Index(name = "idx_graph_custom_props_type", columnList = "customization_type"),
    @Index(name = "idx_graph_custom_props_output", columnList = "output_id"),
    @Index(name = "idx_graph_custom_props_stat_limit", columnList = "statistical_limit_id"),
    @Index(name = "idx_graph_custom_props_det_limit", columnList = "deterministic_limit_id"),
    @Index(name = "idx_graph_custom_props_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstrumentGraphCustomizationPropertiesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "pattern_id", nullable = false)
    @JsonIgnoreProperties({"axes", "properties"})
    private InstrumentGraphPatternEntity pattern;

    @NotNull(message = "Tipo da personalização é obrigatório!")
    @Enumerated(EnumType.STRING)
    @Column(name = "customization_type", nullable = false)
    private CustomizationTypeEnum customizationType;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Cor deve estar no formato hexadecimal válido!")
    @Column(name = "fill_color", nullable = true)
    private String fillColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = true)
    private LineTypeEnum lineType;

    @NotNull(message = "Campo 'Exibir Label' é obrigatório!")
    @Column(name = "label_enable", nullable = false)
    private Boolean labelEnable = false;

    @NotNull(message = "Campo Ordinária Primária é obrigatório!")
    @Column(name = "is_primary_ordinate", nullable = false)
    private Boolean isPrimaryOrdinate = true;

    @ManyToOne
    @JoinColumn(name = "statistical_limit_id", nullable = true)
    @JsonIgnoreProperties("output")
    private StatisticalLimitEntity statisticalLimit;

    @ManyToOne
    @JoinColumn(name = "deterministic_limit_id", nullable = true)
    @JsonIgnoreProperties("output")
    private DeterministicLimitEntity deterministicLimit;

    @ManyToOne
    @JoinColumn(name = "output_id", nullable = true)
    @JsonIgnoreProperties({"instrument", "statisticalLimit", "deterministicLimit"})
    private OutputEntity output;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = true)
    @JsonIgnoreProperties({"inputs", "constants", "outputs", "readings"})
    private InstrumentEntity instrument;
}
