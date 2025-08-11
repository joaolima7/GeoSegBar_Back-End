package com.geosegbar.infra.instrument_graph_customization_properties.dtos;

import java.util.List;

import com.geosegbar.common.enums.LimitValueTypeEnum;

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

    @NotNull(message = "Valores de limites estatísticos são obrigatórios.")
    private List<StatisticalLimitValueReference> statisticalLimitValues;

    @NotNull(message = "Valores de limites determinísticos são obrigatórios.")
    private List<DeterministicLimitValueReference> deterministicLimitValues;

    @NotNull(message = "Campo 'linimetricRulerEnable' é obrigatório")
    private Boolean linimetricRulerEnable;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticalLimitValueReference {

        private Long limitId;
        private LimitValueTypeEnum valueType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeterministicLimitValueReference {

        private Long limitId;
        private LimitValueTypeEnum valueType;
    }
}
