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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "instrument_tabulate_association", indexes = {
    @Index(name = "idx_tabulate_assoc_pattern", columnList = "pattern_id"),
    @Index(name = "idx_tabulate_assoc_instrument", columnList = "instrument_id"),
    @Index(name = "idx_tabulate_assoc_pattern_instrument", columnList = "pattern_id, instrument_id"),
    @Index(name = "idx_tabulate_assoc_date_enable", columnList = "is_date_enable"),
    @Index(name = "idx_tabulate_assoc_indexes", columnList = "date_index, hour_index, user_index")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstrumentTabulateAssociationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pattern_id", nullable = false)
    @NotNull(message = "Padrão de tabela é obrigatório!")
    @JsonIgnoreProperties({"associations"})
    private InstrumentTabulatePatternEntity pattern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    @NotNull(message = "Instrumento é obrigatório!")
    @JsonIgnoreProperties({"inputs", "constants", "outputs", "readings"})
    private InstrumentEntity instrument;

    @Column(name = "is_date_enable", nullable = true)
    private Boolean isDateEnable;

    @Column(name = "date_index", nullable = true)
    private Integer dateIndex;

    @Column(name = "is_hour_enable", nullable = true)
    private Boolean isHourEnable;

    @Column(name = "hour_index", nullable = true)
    private Integer hourIndex;

    @Column(name = "is_user_enable", nullable = true)
    private Boolean isUserEnable;

    @Column(name = "user_index", nullable = true)
    private Integer userIndex;

    @Column(name = "is_read_enable", nullable = true)
    private Boolean isReadEnable;

    @OneToMany(mappedBy = "association", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnoreProperties({"association"})
    private Set<InstrumentTabulateOutputAssociationEntity> outputAssociations = new HashSet<>();
}
