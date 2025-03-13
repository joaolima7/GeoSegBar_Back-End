package com.geosegbar.infra.checklist_response.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateWithAnswersDTO {
    private Long templateId;
    private String templateName;
    private Long questionnaireResponseId;
    private List<QuestionWithAnswerDTO> questionsWithAnswers;
}
