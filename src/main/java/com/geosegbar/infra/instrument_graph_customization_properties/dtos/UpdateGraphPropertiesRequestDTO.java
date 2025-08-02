package com.geosegbar.infra.instrument_graph_customization_properties.dtos;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGraphPropertiesRequestDTO {

    @NotNull(message = "IDs de instrumentos são obrigatórios.")
    private List<Long> instrumentIds;

    @NotNull(message = "IDs de outputs são obrigatórios.")
    private List<Long> outputIds;

    @NotNull(message = "IDs de limites estatísticos são obrigatórios.")
    private List<Long> statisticalLimitIds;

    @NotNull(message = "IDs de limites determinísticos são obrigatórios.")
    private List<Long> deterministicLimitIds;

    @NotNull(message = "Campo 'linimetricRulerEnable' é obrigatório")
    private Boolean linimetricRulerEnable;
}
