package com.geosegbar.infra.instrument_type.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.instrument_type.dtos.InstrumentTypeDTO;
import com.geosegbar.infra.instrument_type.services.InstrumentTypeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/instrument-types")
@RequiredArgsConstructor
public class InstrumentTypeController {

    private final InstrumentTypeService instrumentTypeService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<InstrumentTypeDTO>>> getAllTypes() {
        return ResponseEntity.ok(WebResponseEntity.success(
                instrumentTypeService.findAll(),
                "Tipos de instrumentos obtidos com sucesso!"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<InstrumentTypeDTO>> getTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(WebResponseEntity.success(
                instrumentTypeService.findById(id),
                "Tipo de instrumento obtido com sucesso!"
        ));
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<InstrumentTypeDTO>> createType(
            @Valid @RequestBody InstrumentTypeDTO request) {
        return new ResponseEntity<>(
                WebResponseEntity.success(
                        instrumentTypeService.create(request),
                        "Tipo de instrumento criado com sucesso!"
                ),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<InstrumentTypeDTO>> updateType(
            @PathVariable Long id,
            @Valid @RequestBody InstrumentTypeDTO request) {
        return ResponseEntity.ok(WebResponseEntity.success(
                instrumentTypeService.update(id, request),
                "Tipo de instrumento atualizado com sucesso!"
        ));
    }
}
