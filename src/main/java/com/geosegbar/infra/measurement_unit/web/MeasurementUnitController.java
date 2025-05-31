package com.geosegbar.infra.measurement_unit.web;

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
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.infra.measurement_unit.services.MeasurementUnitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/measurement-units")
@RequiredArgsConstructor
public class MeasurementUnitController {

    private final MeasurementUnitService measurementUnitService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<MeasurementUnitEntity>>> getAllMeasurementUnits() {
        List<MeasurementUnitEntity> units = measurementUnitService.findAll();
        return ResponseEntity.ok(WebResponseEntity.success(units, "Unidades de medida obtidas com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<MeasurementUnitEntity>> getMeasurementUnitById(@PathVariable Long id) {
        MeasurementUnitEntity unit = measurementUnitService.findById(id);
        return ResponseEntity.ok(WebResponseEntity.success(unit, "Unidade de medida obtida com sucesso!"));
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<MeasurementUnitEntity>> createMeasurementUnit(@Valid @RequestBody MeasurementUnitEntity measurementUnit) {
        MeasurementUnitEntity createdUnit = measurementUnitService.create(measurementUnit);
        return new ResponseEntity<>(WebResponseEntity.success(createdUnit, "Unidade de medida criada com sucesso!"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<MeasurementUnitEntity>> updateMeasurementUnit(
            @PathVariable Long id,
            @Valid @RequestBody MeasurementUnitEntity measurementUnit) {
        MeasurementUnitEntity updatedUnit = measurementUnitService.update(id, measurementUnit);
        return ResponseEntity.ok(WebResponseEntity.success(updatedUnit, "Unidade de medida atualizada com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteMeasurementUnit(@PathVariable Long id) {
        measurementUnitService.delete(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Unidade de medida excluída com sucesso!"));
    }
}
