package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "options", indexes = {
    @Index(name = "idx_option_label", columnList = "label", unique = true),
    @Index(name = "idx_option_value", columnList = "value"),
    @Index(name = "idx_option_order", columnList = "order_index")
})
public class OptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório!")
    @Column(name = "label", nullable = false, unique = true)
    private String label;

    @NotBlank(message = "Nome é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "O campo não pode conter números!")
    @Column(name = "value", nullable = true)
    private String value;

    @Column(name = "order_index")
    private Integer orderIndex;

    @JsonProperty(access = Access.WRITE_ONLY)
    @ManyToMany(mappedBy = "selectedOptions", fetch = FetchType.LAZY)
    private Set<AnswerEntity> answers = new HashSet<>();

    @JsonProperty(access = Access.WRITE_ONLY)
    @ManyToMany(mappedBy = "options", fetch = FetchType.LAZY)
    private Set<QuestionEntity> questions = new HashSet<>();
}
