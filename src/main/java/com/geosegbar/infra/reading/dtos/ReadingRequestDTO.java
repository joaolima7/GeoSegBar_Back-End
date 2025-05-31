package com.geosegbar.infra.reading.dtos;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingRequestDTO {

    @NotNull(message = "Data da leitura é obrigatória")
    private LocalDate date;

    @NotNull(message = "Hora da leitura é obrigatória")
    private LocalTime hour;

    @NotNull(message = "Valor da leitura é obrigatório")
    private Double value;
}
