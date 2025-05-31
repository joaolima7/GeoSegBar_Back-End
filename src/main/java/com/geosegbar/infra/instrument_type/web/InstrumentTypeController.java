package com.geosegbar.infra.instrument_type.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.infra.instrument_type.services.InstrumentTypeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/instrument-types")
@RequiredArgsConstructor
public class InstrumentTypeController {

    private final InstrumentTypeService instrumentTypeService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<InstrumentTypeEntity>>> getAllInstrumentTypes() {
        List<InstrumentTypeEntity> types = instrumentTypeService.findAll();
        return ResponseEntity.ok(WebResponseEntity.success(types, "Tipos de instrumento obtidos com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<InstrumentTypeEntity>> getInstrumentTypeById(@PathVariable Long id) {
        InstrumentTypeEntity type = instrumentTypeService.findById(id);
        return ResponseEntity.ok(WebResponseEntity.success(type, "Tipo de instrumento obtido com sucesso!"));
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<InstrumentTypeEntity>> createInstrumentType(@Valid @RequestBody InstrumentTypeEntity instrumentType) {
        InstrumentTypeEntity createdType = instrumentTypeService.create(instrumentType);
        return new ResponseEntity<>(WebResponseEntity.success(createdType, "Tipo de instrumento criado com sucesso!"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<InstrumentTypeEntity>> updateInstrumentType(
            @PathVariable Long id,
            @Valid @RequestBody InstrumentTypeEntity instrumentType) {
        InstrumentTypeEntity updatedType = instrumentTypeService.update(id, instrumentType);
        return ResponseEntity.ok(WebResponseEntity.success(updatedType, "Tipo de instrumento atualizado com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteInstrumentType(@PathVariable Long id) {
        instrumentTypeService.delete(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Tipo de instrumento excluído com sucesso!"));
    }
}
