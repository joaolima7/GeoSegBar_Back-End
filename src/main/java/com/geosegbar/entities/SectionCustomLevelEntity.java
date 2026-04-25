package com.geosegbar.entities;

import java.math.BigDecimal;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "section_custom_level", indexes = {
    @Index(name = "idx_section_custom_level_config", columnList = "config_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SectionCustomLevelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_id", nullable = false)
    @JsonIgnoreProperties({"customLevels", "selectedInstruments", "section"})
    private SectionRenderingConfigEntity config;

    @NotBlank(message = "Nome do nível personalizado é obrigatório!")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Valor do nível personalizado é obrigatório!")
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal value;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Cor deve estar no formato hexadecimal válido!")
    @Column(nullable = true)
    private String color;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean enabled = true;
}
