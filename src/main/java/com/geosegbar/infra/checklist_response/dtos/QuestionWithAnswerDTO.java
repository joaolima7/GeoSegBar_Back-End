package com.geosegbar.infra.checklist_response.dtos;

import java.util.List;

import com.geosegbar.common.enums.TypeQuestionEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionWithAnswerDTO {
    private Long questionId;
    private String questionText;
    private TypeQuestionEnum questionType;
    private Long answerId;
    private String comment;
    private Double latitude;
    private Double longitude;
    private List<OptionInfoDTO> selectedOptions;
    private List<PhotoInfoDTO> photos;
    private List<OptionInfoDTO> allOptions;
}
