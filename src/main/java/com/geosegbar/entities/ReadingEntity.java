package com.geosegbar.entities;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.geosegbar.common.enums.LimitStatusEnum;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reading", indexes = {
    @Index(name = "idx_reading_instrument_date_hour", columnList = "instrument_id, date DESC, hour DESC"),
    @Index(name = "idx_reading_instrument_active_date_hour", columnList = "instrument_id, active, date DESC, hour DESC"),
    @Index(name = "idx_reading_output_active_date_hour", columnList = "output_id, active, date DESC, hour DESC"),
    @Index(name = "idx_reading_instrument_output_active", columnList = "instrument_id, output_id, active"),
    @Index(name = "idx_reading_date_hour_status", columnList = "date DESC, hour DESC, limit_status"),
    @Index(name = "idx_reading_user_date", columnList = "user_id, date DESC"),
    @Index(name = "idx_reading_active", columnList = "active"),
    @Index(name = "idx_reading_limit_status", columnList = "limit_status"),
    @Index(name = "idx_reading_instrument_limit", columnList = "instrument_id, limit_status"),
    @Index(name = "idx_reading_date_range", columnList = "date"),
    @Index(name = "idx_reading_date_instrument_status", columnList = "date DESC, instrument_id, limit_status"),
    @Index(name = "idx_reading_instrument_date", columnList = "instrument_id, date DESC"),
    @Index(name = "idx_reading_output_date", columnList = "output_id, date DESC"),
    @Index(name = "idx_reading_user_instrument", columnList = "user_id, instrument_id"),
    @Index(name = "idx_reading_date_time_combined", columnList = "date DESC, hour DESC"),
    @Index(name = "idx_reading_instrument_value", columnList = "instrument_id, calculated_value"),
    @Index(name = "idx_reading_output_value", columnList = "output_id, calculated_value"),
    @Index(name = "idx_reading_active_date", columnList = "active, date DESC"),
    @Index(name = "idx_reading_status_date", columnList = "limit_status, date DESC"),
    @Index(name = "idx_reading_instrument_output_date", columnList = "instrument_id, output_id, date DESC"),
    @Index(name = "idx_reading_user_active_date", columnList = "user_id, active, date DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Data da leitura é obrigatória!")
    @Column(nullable = false)
    private LocalDate date;

    @NotNull(message = "Hora da leitura é obrigatória!")
    @Column(nullable = false)
    private LocalTime hour;

    @Column(nullable = false)
    private Double calculatedValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LimitStatusEnum limitStatus;

    @Column(nullable = false)
    private Boolean active;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"readings", "password", "damPermissions", "createdUsers", "psbFoldersCreated",
        "psbFilesUploaded", "sharedFolders", "attributionsPermission",
        "documentationPermission", "instrumentationPermission", "routineInspectionPermission"})
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    @JsonIgnoreProperties({"readings", "inputs", "outputs", "constants", "statisticalLimit", "deterministicLimit"})
    private InstrumentEntity instrument;

    @ManyToOne
    @JoinColumn(name = "output_id", nullable = false)
    @JsonIgnoreProperties({"instrument"})
    private OutputEntity output;

    @OneToMany(mappedBy = "reading", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties("reading")
    private Set<ReadingInputValueEntity> inputValues = new HashSet<>();
}
