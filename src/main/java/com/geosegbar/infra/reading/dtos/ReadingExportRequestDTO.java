package com.geosegbar.infra.reading.dtos;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingExportRequestDTO {

    @NotEmpty(message = "É necessário fornecer pelo menos um ID de instrumento")
    @Size(max = 50, message = "Máximo de 50 instrumentos permitidos por exportação")
    private List<Long> instrumentIds;

    private LocalDate startDate;

    private LocalDate endDate;
}
