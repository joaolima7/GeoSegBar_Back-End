package com.geosegbar.infra.instrument_graph_axes.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.instrument_graph_axes.dtos.GraphAxesResponseDTO;
import com.geosegbar.infra.instrument_graph_axes.dtos.UpdateGraphAxesRequestDTO;
import com.geosegbar.infra.instrument_graph_axes.services.InstrumentGraphAxesService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/graph-axes")
@RequiredArgsConstructor
public class InstrumentGraphAxesController {

    private final InstrumentGraphAxesService axesService;

    @PutMapping("/pattern/{patternId}")
    public ResponseEntity<WebResponseEntity<GraphAxesResponseDTO>> updateAxes(
            @PathVariable Long patternId,
            @Valid @RequestBody UpdateGraphAxesRequestDTO request) {

        GraphAxesResponseDTO dto = axesService.updateAxes(patternId, request);
        return ResponseEntity.ok(
                WebResponseEntity.success(dto, "Eixos atualizados com sucesso!"));
    }
}
