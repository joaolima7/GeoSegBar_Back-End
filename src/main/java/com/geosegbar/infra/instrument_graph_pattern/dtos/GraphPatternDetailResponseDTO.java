package com.geosegbar.infra.instrument_graph_pattern.dtos;

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
public class GraphPatternDetailResponseDTO {

    private Long id;
    private String name;
    private InstrumentDetailDTO instrument;
    private FolderDetailDTO folder;
    private AxesDetailDTO axes;
    private List<PropertyDetailDTO> properties;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FolderDetailDTO {

        private Long id;
        private String name;
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
    public static class AxesDetailDTO {

        private Long id;
        private Integer abscissaPx;
        private Boolean abscissaGridLinesEnable;
        private Integer primaryOrdinatePx;
        private Integer secondaryOrdinatePx;
        private Boolean primaryOrdinateGridLinesEnable;
        private String primaryOrdinateTitle;
        private String secondaryOrdinateTitle;
        private Double primaryOrdinateSpacing;
        private Double secondaryOrdinateSpacing;
        private Double primaryOrdinateInitialValue;
        private Double secondaryOrdinateInitialValue;
        private Double primaryOrdinateMaximumValue;
        private Double secondaryOrdinateMaximumValue;
    }

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
        private RelatedInstrumentDTO instrument;
        private RelatedOutputDTO output;
        private RelatedConstantDTO constant;
        private RelatedStatisticalLimitDTO statisticalLimit;
        private RelatedDeterministicLimitDTO deterministicLimit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedInstrumentDTO {

        private Long id;
        private String name;
        private String location;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedOutputDTO {

        private Long id;
        private String acronym;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedStatisticalLimitDTO {

        private Long id;
        private Double lowerValue;
        private Double upperValue;
        private RelatedOutputDTO output;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedDeterministicLimitDTO {

        private Long id;
        private Double attentionValue;
        private Double alertValue;
        private Double emergencyValue;
        private RelatedOutputDTO output;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedConstantDTO {

        private Long id;
        private String acronym;
        private String name;
        private Double value;
    }
}
