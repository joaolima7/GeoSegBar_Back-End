package com.geosegbar.infra.instrument_graph_customization_properties.dtos;

import java.util.List;

import com.geosegbar.common.enums.LimitValueTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePropertiesBatchRequestDTO {

    @NotEmpty(message = "Lista de propriedades não pode estar vazia")
    @Valid
    private List<PropertyUpdateItem> properties;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyUpdateItem {

        @NotNull(message = "ID da propriedade é obrigatório")
        private Long id;

        private String name;

        @jakarta.validation.constraints.Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Cor deve estar no formato hexadecimal válido!")
        private String fillColor;

        private LineTypeEnum lineType;

        @NotNull(message = "Campo 'Exibir Label' é obrigatório!")
        private Boolean labelEnable;

        @NotNull(message = "Campo 'Ordinária Primária' é obrigatório!")
        private Boolean isPrimaryOrdinate;

        private LimitValueTypeEnum limitValueType;
    }
}
