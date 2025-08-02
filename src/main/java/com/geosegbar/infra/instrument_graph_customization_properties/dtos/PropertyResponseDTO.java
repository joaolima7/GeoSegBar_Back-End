package com.geosegbar.infra.instrument_graph_customization_properties.dtos;

import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyResponseDTO {

    private Long id;
    private String name;
    private CustomizationTypeEnum customizationType;
    private String fillColor;
    private LineTypeEnum lineType;
    private Boolean labelEnable;
    private Boolean isPrimaryOrdinate;
    private Long patternId;

    private Long instrumentId;
    private Long outputId;
    private Long statisticalLimitId;
    private Long deterministicLimitId;
}
