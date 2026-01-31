package com.geosegbar.entities;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "deterministic_limit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeterministicLimitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private BigDecimal attentionValue;

    @Column(nullable = true)
    private BigDecimal alertValue;

    @Column(nullable = true)
    private BigDecimal emergencyValue;

    @OneToOne
    @JoinColumn(name = "output_id", nullable = false)
    @JsonIgnoreProperties("deterministicLimit")
    private OutputEntity output;
}
