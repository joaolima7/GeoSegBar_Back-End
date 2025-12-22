package com.geosegbar.infra.checklist.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistWithLastAnswersAndDamDTO {

    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private List<TemplateQuestionnaireWithAnswersDTO> templateQuestionnaires;
    private DamInfoDTO dam;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamInfoDTO {

        private Long id;
        private String name;
        private String city;
        private String state;
        private Double latitude;
        private Double longitude;
    }
}
