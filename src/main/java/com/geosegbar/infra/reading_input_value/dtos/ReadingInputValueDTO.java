package com.geosegbar.infra.reading_input_value.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingInputValueDTO {

    private String inputAcronym;
    private String inputName;
    private Double value;
}
