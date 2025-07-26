package com.geosegbar.infra.instrument.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Parâmetros fixos para todos os instrumentos na importação
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportInstrumentsRequest {

    @NotNull(message = "ID da barragem é obrigatório")
    private Long damId;

    @NotNull(message = "Campo 'Sem limites' é obrigatório")
    private Boolean noLimit;

    @NotNull(message = "Campo 'Visível na seção' é obrigatório")
    private Boolean activeForSection;
}
