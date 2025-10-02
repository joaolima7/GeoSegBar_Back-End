package com.geosegbar.infra.instrument.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentResponseDTO {

    private Long id;
    private String name;
    private String location;
    private Double distanceOffset;
    private Double latitude;
    private Double longitude;
    private Boolean noLimit;
    private Boolean active;
    private Boolean activeForSection;
    private Boolean isLinimetricRuler;
    private Long linimetricRulerCode;
    private Long damId;
    private String damName;
    private Long instrumentTypeId;
    private String instrumentType;
    private Long sectionId;
    private String sectionName;
    private LocalDateTime lastUpdateVariablesDate;
    private StatisticalLimitDTO statisticalLimit;
    private DeterministicLimitDTO deterministicLimit;
    private List<InputDTO> inputs;
    private List<ConstantDTO> constants;
    private List<OutputDTO> outputs;
}
