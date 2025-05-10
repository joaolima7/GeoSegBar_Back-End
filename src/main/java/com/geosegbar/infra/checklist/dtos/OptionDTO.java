package com.geosegbar.infra.checklist.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionDTO {

    private Long id;
    private String label;
    private String value;
}
