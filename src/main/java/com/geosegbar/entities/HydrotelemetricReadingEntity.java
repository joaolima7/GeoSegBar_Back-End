package com.geosegbar.entities;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hydrotelemetric_reading")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "downstream_average", nullable = false)
    @NotNull(message = "A média da jusante é obrigatória!")
    private Double downstreamAverage;

    @Column(name = "upstream_average", nullable = false)
    @NotNull(message = "A média da montante é obrigatória!")
    private Double upstreamAverage;
}
