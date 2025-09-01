package com.geosegbar.infra.instrument_type.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentTypeDTO {

    private Long id;

    @NotBlank(message = "Nome do tipo de instrumento é obrigatório")
    private String name;

}
