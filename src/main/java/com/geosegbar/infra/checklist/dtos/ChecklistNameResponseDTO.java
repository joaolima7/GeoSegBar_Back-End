package com.geosegbar.infra.checklist.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChecklistNameResponseDTO {

    private Long id;
    private String name;
    private Long damId;
}
