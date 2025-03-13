package com.geosegbar.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "questionnaire_responses")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QuestionnaireResponseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Informe o modelo de questionário que esta sendo respondido!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_questionnaire_id", nullable = false)
    private TemplateQuestionnaireEntity templateQuestionnaire;

    @NotNull(message = "Informe a barragem que corresponde a esse checklist!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dam_id", nullable = false)
    private DamEntity dam;

    @NotNull(message = "Informe a resposta de checklist à qual este questionário pertence!")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_response_id", nullable = false)
    @JsonBackReference(value = "checklist-questionnaire-responses")
    private ChecklistResponseEntity checklistResponse;

    @Column(name = "created_at", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "questionnaireResponse", fetch = FetchType.LAZY,
               cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "questionnaire-response-answers")
    private Set<AnswerEntity> answers = new HashSet<>();
           
}
