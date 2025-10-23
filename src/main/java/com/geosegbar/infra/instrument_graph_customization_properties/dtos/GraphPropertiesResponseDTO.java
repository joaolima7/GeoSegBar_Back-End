package com.geosegbar.infra.instrument_graph_customization_properties.dtos;

import java.util.List;

import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LimitValueTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphPropertiesResponseDTO {

    private Long patternId;
    private List<PropertyDetailDTO> properties;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyDetailDTO {

        private Long id;
        private String name;
        private CustomizationTypeEnum customizationType;
        private String fillColor;
        private LineTypeEnum lineType;
        private Boolean labelEnable;
        private Boolean isPrimaryOrdinate;
        private LimitValueTypeEnum limitValueType;

        private InstrumentDetailDTO instrument;
        private OutputDetailDTO output;
        private ConstantDetailDTO constant;
        private StatisticalLimitDetailDTO statisticalLimit;
        private DeterministicLimitDetailDTO deterministicLimit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstrumentDetailDTO {

        private Long id;
        private String name;
        private String location;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputDetailDTO {

        private Long id;
        private String acronym;
        private String name;
        private MeasurementUnitDTO measurementUnit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticalLimitDetailDTO {

        private Long id;
        private Double lowerValue;
        private Double upperValue;
        private OutputDetailDTO output;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeterministicLimitDetailDTO {

        private Long id;
        private Double attentionValue;
        private Double alertValue;
        private Double emergencyValue;
        private OutputDetailDTO output;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConstantDetailDTO {

        private Long id;
        private String acronym;
        private String name;
        private Double value;
        private MeasurementUnitDTO measurementUnit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeasurementUnitDTO {

        private Long id;
        private String name;
        private String acronym;
    }
}
