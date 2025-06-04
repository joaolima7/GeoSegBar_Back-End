package com.geosegbar.infra.instrument.dtos;

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
    private Long damId;
    private String damName;
    private String instrumentType;
    private Long sectionId;
    private String sectionName;
    private StatisticalLimitDTO statisticalLimit;
    private DeterministicLimitDTO deterministicLimit;
    private List<InputDTO> inputs;
    private List<ConstantDTO> constants;
    private List<OutputDTO> outputs;
}
