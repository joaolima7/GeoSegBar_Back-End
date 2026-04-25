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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "section_rendering_config", indexes = {
    @Index(name = "idx_section_rendering_section", columnList = "section_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SectionRenderingConfigEntity {

    private static final String HEX_COLOR_REGEX = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
    private static final String HEX_COLOR_MSG = "Cor deve estar no formato hexadecimal válido!";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"renderingConfig", "instruments", "dam"})
    private SectionEntity section;

    @Column(name = "soil_label", nullable = true)
    private String soilLabel;

    @Pattern(regexp = HEX_COLOR_REGEX, message = HEX_COLOR_MSG)
    @Column(name = "soil_color", nullable = true)
    private String soilColor;

    @Column(name = "filter_label", nullable = true)
    private String filterLabel;

    @Pattern(regexp = HEX_COLOR_REGEX, message = HEX_COLOR_MSG)
    @Column(name = "filter_color", nullable = true)
    private String filterColor;

    @Column(name = "rock_label", nullable = true)
    private String rockLabel;

    @Pattern(regexp = HEX_COLOR_REGEX, message = HEX_COLOR_MSG)
    @Column(name = "rock_color", nullable = true)
    private String rockColor;

    @Pattern(regexp = HEX_COLOR_REGEX, message = HEX_COLOR_MSG)
    @Column(name = "top_elevation_color", nullable = true)
    private String topElevationColor;

    @Pattern(regexp = HEX_COLOR_REGEX, message = HEX_COLOR_MSG)
    @Column(name = "bottom_elevation_color", nullable = true)
    private String bottomElevationColor;

    @Pattern(regexp = HEX_COLOR_REGEX, message = HEX_COLOR_MSG)
    @Column(name = "piezometric_elevation_color", nullable = true)
    private String piezometricElevationColor;

    @Column(name = "axis_x_min", nullable = true)
    private Double axisXMin;

    @Column(name = "axis_x_max", nullable = true)
    private Double axisXMax;

    @Column(name = "axis_y_min", nullable = true)
    private Double axisYMin;

    @Column(name = "axis_y_max", nullable = true)
    private Double axisYMax;

    @Column(name = "show_dam_axis", nullable = false, columnDefinition = "boolean default false")
    private Boolean showDamAxis = false;

    @Column(name = "show_last_upstream_reading", nullable = false, columnDefinition = "boolean default false")
    private Boolean showLastUpstreamReading = false;

    @Column(name = "show_last_downstream_reading", nullable = false, columnDefinition = "boolean default false")
    private Boolean showLastDownstreamReading = false;

    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("config")
    private Set<SectionCustomLevelEntity> customLevels = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "section_rendering_selected_instruments",
            joinColumns = @JoinColumn(name = "config_id"),
            inverseJoinColumns = @JoinColumn(name = "instrument_id"),
            indexes = {
                @Index(name = "idx_srsi_config", columnList = "config_id"),
                @Index(name = "idx_srsi_instrument", columnList = "instrument_id")
            }
    )
    @JsonIgnoreProperties({"readings", "inputs", "constants", "outputs", "dam", "section"})
    private Set<InstrumentEntity> selectedInstruments = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "section_rendering_selected_reservoirs",
            joinColumns = @JoinColumn(name = "config_id"),
            inverseJoinColumns = @JoinColumn(name = "reservoir_id"),
            indexes = {
                @Index(name = "idx_srsr_config", columnList = "config_id"),
                @Index(name = "idx_srsr_reservoir", columnList = "reservoir_id")
            }
    )
    @JsonIgnoreProperties({"dam", "reservoirs"})
    private Set<ReservoirEntity> selectedReservoirs = new HashSet<>();
}
