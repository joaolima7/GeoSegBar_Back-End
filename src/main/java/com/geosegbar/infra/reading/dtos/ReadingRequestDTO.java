package com.geosegbar.infra.reading.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import jakarta.validation.constraints.NotEmpty;
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

    @NotEmpty(message = "É necessário fornecer valores para todos os inputs")
    private Map<String, Double> inputValues; // Mapa de acrônimo do input -> valor
}
