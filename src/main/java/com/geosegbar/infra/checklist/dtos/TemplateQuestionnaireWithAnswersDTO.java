package com.geosegbar.infra.checklist.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQuestionnaireWithAnswersDTO {

    private Long id;
    private String name;
    private List<QuestionWithLastAnswerDTO> questions;
}
