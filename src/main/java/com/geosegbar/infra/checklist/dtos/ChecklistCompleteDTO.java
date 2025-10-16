package com.geosegbar.infra.checklist.dtos;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistCompleteDTO {

    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private Set<TemplateQuestionnaireDTO> templateQuestionnaires = new HashSet<>();
    private Set<DamDTO> dams = new HashSet<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateQuestionnaireDTO {

        private Long id;
        private String name;
        private Set<TemplateQuestionnaireQuestionDTO> templateQuestions = new HashSet<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateQuestionnaireQuestionDTO {

        private Long id;
        private QuestionDTO question;
        private Integer orderIndex;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionDTO {

        private Long id;
        private String questionText;
        private String type;
        private Set<OptionDTO> options = new HashSet<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDTO {

        private Long id;
        private String label;
        private String value;
        private Integer orderIndex;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamDTO {

        private Long id;
        private String name;
        private Double latitude;
        private Double longitude;
        private ClientDTO client;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientDTO {

        private Long id;
        private String name;
    }
}
