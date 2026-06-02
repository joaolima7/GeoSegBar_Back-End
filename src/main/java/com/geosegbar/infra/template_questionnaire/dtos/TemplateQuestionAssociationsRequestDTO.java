package com.geosegbar.infra.template_questionnaire.dtos;

import java.util.List;

import com.geosegbar.infra.template_questionnaire_question.dtos.QuestionOrderDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionAssociationsRequestDTO {

    private List<Long> associateQuestionIds;

    private List<Long> disassociateQuestionIds;

    @NotNull(message = "Lista de ordenação final é obrigatória!")
    @Valid
    private List<QuestionOrderDTO> order;
}
