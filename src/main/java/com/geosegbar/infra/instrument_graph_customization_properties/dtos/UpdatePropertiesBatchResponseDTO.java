package com.geosegbar.infra.instrument_graph_customization_properties.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePropertiesBatchResponseDTO {

    private Long patternId;
    private Integer totalUpdated;
    private List<PropertyResponseDTO> updatedProperties;
    private List<PropertyUpdateError> errors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyUpdateError {
        private Long propertyId;
        private String error;
    }
}