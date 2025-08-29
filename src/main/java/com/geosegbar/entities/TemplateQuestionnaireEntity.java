package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "template_questionnaires", indexes = {
    @Index(name = "idx_template_questionnaire_name", columnList = "name")
})
public class TemplateQuestionnaireEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome do Modelo de Questionário é obrigatório!")
    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(mappedBy = "templateQuestionnaire", fetch = FetchType.EAGER,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<TemplateQuestionnaireQuestionEntity> templateQuestions = new HashSet<>();

    @ManyToMany(mappedBy = "templateQuestionnaires", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ChecklistEntity> checklists = new HashSet<>();
}
