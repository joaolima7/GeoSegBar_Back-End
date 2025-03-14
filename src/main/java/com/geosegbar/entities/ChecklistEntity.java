package com.geosegbar.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
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
@Table(name = "checklists")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChecklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome do checklist é obrigatório!")
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "checklist_template_questionnaire",
        joinColumns = @JoinColumn(name = "checklist_id"),
        inverseJoinColumns = @JoinColumn(name = "template_questionnaire_id"))
    private Set<TemplateQuestionnaireEntity> templateQuestionnaires = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "checklist_dam",
        joinColumns = @JoinColumn(name = "checklist_id"),
        inverseJoinColumns = @JoinColumn(name = "dam_id"))
    private Set<DamEntity> dams = new HashSet<>();
}
