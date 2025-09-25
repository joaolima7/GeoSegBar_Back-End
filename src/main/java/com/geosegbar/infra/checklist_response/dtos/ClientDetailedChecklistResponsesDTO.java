package com.geosegbar.infra.checklist_response.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDetailedChecklistResponsesDTO {

    private Long clientId;
    private String clientName;
    private List<ChecklistWithDetailedResponsesDTO> checklists;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChecklistWithDetailedResponsesDTO {

        private Long checklistId;
        private String checklistName;
        private List<ChecklistResponseDetailDTO> latestResponses;
    }
}
