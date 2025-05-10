package com.geosegbar.infra.checklist.dtos;

import java.util.List;

import com.geosegbar.common.enums.TypeQuestionEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionWithLastAnswerDTO {

    private Long id;
    private String questionText;
    private TypeQuestionEnum type;
    private OptionDTO lastSelectedOption;
    private List<OptionDTO> allOptions;
    private Long answerResponseId;
}
