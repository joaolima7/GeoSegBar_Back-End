package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reading_input_value")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadingInputValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Acrônimo do input é obrigatório!")
    @Column(nullable = false)
    private String inputAcronym;

    @NotBlank(message = "Nome do input é obrigatório!")
    @Column(nullable = false)
    private String inputName;

    @NotNull(message = "Valor do input é obrigatório!")
    @Column(nullable = false)
    private Double value;

    @ManyToOne
    @JoinColumn(name = "reading_id", nullable = false)
    @JsonIgnoreProperties({"inputValues"})
    private ReadingEntity reading;
}
