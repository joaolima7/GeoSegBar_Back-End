package com.geosegbar.infra.checklist.dtos;

import java.util.List;

import com.geosegbar.infra.template_questionnaire.dtos.TemplateQuestionDTO;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateInChecklistDTO {

    /**
     * ID do template existente (se estiver editando ou reutilizando). Se for
     * null, será criado um novo template.
     */
    private Long templateId;

    /**
     * Nome do template (obrigatório apenas se for criar um novo).
     */
    private String name;

    /**
     * Lista de questões do template. Cada questão pode ter um questionId
     * (existente) ou dados para criar uma nova. Obrigatório apenas se for criar
     * um novo template.
     */
    @Valid
    private List<TemplateQuestionDTO> questions;

    /**
     * Verifica se é um template existente.
     */
    public boolean isExistingTemplate() {
        return templateId != null;
    }

    /**
     * Verifica se é um novo template a ser criado.
     */
    public boolean isNewTemplate() {
        return templateId == null;
    }
}
