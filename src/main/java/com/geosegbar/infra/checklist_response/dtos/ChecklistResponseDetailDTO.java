package com.geosegbar.infra.checklist_response.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistResponseDetailDTO {

    private Long id;
    private String checklistName;
    private Long checklistId;
    private LocalDateTime createdAt;
    private List<TemplateWithAnswersDTO> templates;
    private Long userId;
    private String userName;
    private DamInfoDTO dam;
}
