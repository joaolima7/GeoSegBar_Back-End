package com.geosegbar.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "checklists",
        indexes = {
            @Index(name = "idx_checklist_name", columnList = "name"),
            @Index(name = "idx_checklist_created_at", columnList = "created_at"),
            @Index(name = "idx_checklist_dam_id", columnList = "dam_id")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_checklist_name_dam", columnNames = {"name", "dam_id"})
        }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChecklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome do checklist é obrigatório!")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "checklist", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @JsonManagedReference
    private List<ChecklistTemplateEntity> checklistTemplates = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dam_id", nullable = false)
    private DamEntity dam;

    // Getter JSON backward-compat: mantém "templateQuestionnaires" no JSON do GET /{id}
    @JsonProperty("templateQuestionnaires")
    public Set<TemplateQuestionnaireEntity> getTemplateQuestionnairesForJson() {
        if (checklistTemplates == null) {
            return new HashSet<>();
        }
        return checklistTemplates.stream()
                .map(ChecklistTemplateEntity::getTemplateQuestionnaire)
                .collect(Collectors.toSet());
    }
}
