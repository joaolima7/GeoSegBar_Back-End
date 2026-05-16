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
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "checklist_templates",
        indexes = {
            @Index(name = "idx_ct_checklist_id", columnList = "checklist_id"),
            @Index(name = "idx_ct_template_id", columnList = "template_questionnaire_id"),
            @Index(name = "idx_ct_checklist_order", columnList = "checklist_id, order_index")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_ct_checklist_template",
                    columnNames = {"checklist_id", "template_questionnaire_id"})
        }
)
public class ChecklistTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false)
    @JsonBackReference
    private ChecklistEntity checklist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_questionnaire_id", nullable = false)
    private TemplateQuestionnaireEntity templateQuestionnaire;

    @Column(name = "order_index", nullable = false)
    @NotNull(message = "Índice de ordem é obrigatório!")
    private Integer orderIndex;
}
