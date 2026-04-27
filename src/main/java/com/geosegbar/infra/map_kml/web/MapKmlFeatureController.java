package com.geosegbar.infra.map_kml.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.map_kml.dtos.KmlFeatureDTO;
import com.geosegbar.infra.map_kml.dtos.KmlFeatureUpdateRequest;
import com.geosegbar.infra.map_kml.services.KmlProcessingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/map-kml/files")
@RequiredArgsConstructor
public class MapKmlFeatureController {

    private final KmlProcessingService kmlProcessingService;

    @GetMapping("/{fileId}/features")
    public ResponseEntity<WebResponseEntity<List<KmlFeatureDTO>>> getFeatures(
            @PathVariable Long fileId,
            @RequestParam(required = false) String type) {
        List<KmlFeatureDTO> features = kmlProcessingService.getFeatures(fileId, type);
        return ResponseEntity.ok(WebResponseEntity.success(features, "Features KML obtidas com sucesso!"));
    }

    @PatchMapping("/{fileId}/features/{featureIndex}")
    public ResponseEntity<WebResponseEntity<KmlFeatureDTO>> updateFeature(
            @PathVariable Long fileId,
            @PathVariable Integer featureIndex,
            @Valid @RequestBody KmlFeatureUpdateRequest request) {
        KmlFeatureDTO dto = kmlProcessingService.updateFeature(fileId, featureIndex, request);
        return ResponseEntity.ok(WebResponseEntity.success(dto, "Feature atualizada com sucesso!"));
    }
}
