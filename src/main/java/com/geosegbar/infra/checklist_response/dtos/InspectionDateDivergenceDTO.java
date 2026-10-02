package com.geosegbar.infra.checklist_response.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta da prévia de mudança de data: o que PASSARIA a estar irregular se a
 * inspeção fosse movida. Lista vazia significa que pode gravar direto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionDateDivergenceDTO {

    private LocalDateTime currentDate;
    private LocalDateTime newDate;
    private List<Item> divergences;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        private Long checklistResponseId;
        private LocalDateTime inspectionDate;
        private Long templateQuestionnaireId;
        private Long questionId;
        private String questionText;
        private String label;
        private String previousLabelBefore;
        private String previousLabelAfter;
        private String reason;
    }
}
