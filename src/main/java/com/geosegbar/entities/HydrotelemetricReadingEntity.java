package com.geosegbar.entities;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.geosegbar.common.enums.ReadingTypeEnum;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "hydrotelemetric_reading", indexes = {
    @Index(name = "idx_hydrotelemetric_dam_id", columnList = "dam_id"),
    @Index(name = "idx_hydrotelemetric_date", columnList = "date"),
    @Index(name = "idx_hydrotelemetric_dam_date", columnList = "dam_id, date")
})
public class HydrotelemetricReadingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "dam_id", nullable = false)
    @NotNull(message = "A barragem é obrigatória!")
    private DamEntity dam;

    @Column(nullable = false)
    @NotNull(message = "A data da leitura é obrigatória!")
    private LocalDate date;

    @Column(name = "downstream_average")
    private Double downstreamAverage;

    @Column(name = "upstream_average")
    private Double upstreamAverage;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_type", nullable = false)
    @NotNull(message = "O tipo de leitura é obrigatório!")
    private ReadingTypeEnum readingType;
}
