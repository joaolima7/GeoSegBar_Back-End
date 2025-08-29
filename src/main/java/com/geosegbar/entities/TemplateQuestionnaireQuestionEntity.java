package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "template_questionnaire_questions", indexes = {
    @Index(name = "idx_tqq_template_id", columnList = "template_questionnaire_id"),
    @Index(name = "idx_tqq_question_id", columnList = "question_id"),
    @Index(name = "idx_tqq_order", columnList = "template_questionnaire_id, order_index")
})
public class TemplateQuestionnaireQuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_questionnaire_id", nullable = false)
    @JsonBackReference
    private TemplateQuestionnaireEntity templateQuestionnaire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    @Column(name = "order_index")
    @NotNull(message = "Índice de ordem é obrigatório!")
    private Integer orderIndex;
}
