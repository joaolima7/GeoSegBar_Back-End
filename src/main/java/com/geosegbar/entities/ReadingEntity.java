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
@Table(name = "reading")
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
