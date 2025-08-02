package com.geosegbar.infra.instrument_graph_customization_properties.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.GraphPropertiesResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.PropertyResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdateGraphPropertiesRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdatePropertiesBatchRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdatePropertiesBatchResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdatePropertyRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.services.InstrumentGraphCustomizationPropertiesService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/graph-properties")
@RequiredArgsConstructor
public class InstrumentGraphCustomizationPropertiesController {

    private final InstrumentGraphCustomizationPropertiesService propertiesService;

    @PutMapping("/elements/pattern/{patternId}")
    public ResponseEntity<WebResponseEntity<Void>> updateProperties(
            @PathVariable Long patternId,
            @Valid @RequestBody UpdateGraphPropertiesRequestDTO request) {

        propertiesService.updateProperties(patternId, request);
        return ResponseEntity.ok(
                WebResponseEntity.success(null, "Propriedades atualizadas com sucesso!"));
    }

    @PutMapping("/{propertyId}")
    public ResponseEntity<WebResponseEntity<PropertyResponseDTO>> updateProperty(
            @PathVariable Long propertyId,
            @Valid @RequestBody UpdatePropertyRequestDTO request) {

        PropertyResponseDTO dto = propertiesService.updateProperty(propertyId, request);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Propriedade atualizada com sucesso!"));
    }

    @PutMapping("/batch/pattern/{patternId}")
    public ResponseEntity<WebResponseEntity<UpdatePropertiesBatchResponseDTO>> updatePropertiesBatch(
            @PathVariable Long patternId,
            @Valid @RequestBody UpdatePropertiesBatchRequestDTO request) {

        UpdatePropertiesBatchResponseDTO response = propertiesService.updatePropertiesBatch(patternId, request);

        String message = String.format("Processamento concluído: %d propriedades atualizadas",
                response.getTotalUpdated());

        if (!response.getErrors().isEmpty()) {
            message += String.format(", %d erros encontrados", response.getErrors().size());
        }

        return ResponseEntity.ok(WebResponseEntity.success(response, message));
    }

    @GetMapping("/pattern/{patternId}")
    public ResponseEntity<WebResponseEntity<GraphPropertiesResponseDTO>> getPropertiesByPattern(
            @PathVariable Long patternId) {

        GraphPropertiesResponseDTO dto = propertiesService.findByPatternId(patternId);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Propriedades recuperadas com sucesso!"));
    }
}
