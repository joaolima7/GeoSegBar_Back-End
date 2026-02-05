package com.geosegbar.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;

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
    @Index(name = "idx_reading_instrument_active_date_hour", columnList = "instrument_id, active, date DESC, hour DESC"),

    @Index(name = "idx_reading_output_active_date_hour", columnList = "output_id, active, date DESC, hour DESC"),

    @Index(name = "idx_reading_active_date_hour", columnList = "active, date DESC, hour DESC"),})
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

    @NotNull(message = "Valor calculado é obrigatório!")
    @Column(nullable = false)
    private BigDecimal calculatedValue;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    @JsonIgnoreProperties({"readings", "inputs", "outputs", "constants", "statisticalLimit", "deterministicLimit"})
    private InstrumentEntity instrument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "output_id", nullable = false)
    @JsonIgnoreProperties({"instrument"})
    private OutputEntity output;

    @OneToMany(mappedBy = "reading", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"reading"})
    @BatchSize(size = 500)
    private Set<ReadingInputValueEntity> inputValues = new HashSet<>();
}
