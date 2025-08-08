package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "instrument_tabulate_output_association", indexes = {
    @Index(name = "idx_tabulate_output_assoc_association", columnList = "association_id"),
    @Index(name = "idx_tabulate_output_assoc_output", columnList = "output_id"),
    @Index(name = "idx_tabulate_output_assoc_index", columnList = "output_index"),
    @Index(name = "idx_tabulate_output_assoc_assoc_output", columnList = "association_id, output_id"),
    @Index(name = "idx_tabulate_output_assoc_assoc_index", columnList = "association_id, output_index")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstrumentTabulateOutputAssociationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "association_id", nullable = false)
    @NotNull(message = "Associação de instrumento é obrigatória!")
    @JsonIgnoreProperties({"outputAssociations"})
    private InstrumentTabulateAssociationEntity association;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "output_id", nullable = false)
    @NotNull(message = "Output é obrigatório!")
    @JsonIgnoreProperties({"instrument", "statisticalLimit", "deterministicLimit"})
    private OutputEntity output;

    @NotNull(message = "Índice do output na tabela é obrigatório!")
    @Column(name = "output_index", nullable = false)
    private Integer outputIndex;
}
