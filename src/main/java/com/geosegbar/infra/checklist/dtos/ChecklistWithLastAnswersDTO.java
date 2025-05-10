package com.geosegbar.infra.checklist.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistWithLastAnswersDTO {

    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private List<TemplateQuestionnaireWithAnswersDTO> templateQuestionnaires;
}
