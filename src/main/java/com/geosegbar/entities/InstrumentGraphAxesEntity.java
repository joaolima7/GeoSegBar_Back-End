package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instrument_graph_axes", indexes = {
    @Index(name = "idx_graph_axes_pattern", columnList = "pattern_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstrumentGraphAxesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "pattern_id", nullable = false)
    @JsonIgnoreProperties({"elements", "customization", "axes"})
    private InstrumentGraphPatternEntity pattern;

    @NotNull(message = "Tamanho da fonte da abcissa é obrigatório!")
    @Column(name = "abscissa_px", nullable = false)
    private Integer abscissaPx;

    @NotNull(message = "Linhas de Grade da Abcissa é obrigatório!")
    @Column(name = "abscissa_grid_lines_enable", nullable = false)
    private Boolean abscissaGridLinesEnable;

    @NotNull(message = "Tamanho da fonte da ordenada primária é obrigatório!")
    @Column(name = "primary_ordinate_px", nullable = false)
    private Integer primaryOrdinatePx;

    @NotNull(message = "Tamanho da fonte da ordenada secundária é obrigatório!")
    @Column(name = "secondary_ordinate_px", nullable = false)
    private Integer secondaryOrdinatePx;

    @NotNull(message = "Linhas de Grade da Ordenada Primária é obrigatório!")
    @Column(name = "primary_ordinate_grid_lines_enable", nullable = false)
    private Boolean primaryOrdinateGridLinesEnable;

    @Column(name = "primary_ordinate_title", nullable = true)
    private String primaryOrdinateTitle;

    @Column(name = "secondary_ordinate_title", nullable = true)
    private String secondaryOrdinateTitle;

    @Column(name = "primary_ordinate_spacing", nullable = true)
    private Double primaryOrdinateSpacing;

    @Column(name = "secondary_ordinate_spacing", nullable = true)
    private Double secondaryOrdinateSpacing;

    @Column(name = "primary_ordinate_initial_value", nullable = true)
    private Double primaryOrdinateInitialValue;

    @Column(name = "secondary_ordinate_initial_value", nullable = true)
    private Double secondaryOrdinateInitialValue;

    @Column(name = "primary_ordinate_maximum_value", nullable = true)
    private Double primaryOrdinateMaximumValue;

    @Column(name = "secondary_ordinate_maximum_value", nullable = true)
    private Double secondaryOrdinateMaximumValue;
}
