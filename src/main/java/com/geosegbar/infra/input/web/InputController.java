package com.geosegbar.infra.input.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.InputEntity;
import com.geosegbar.infra.input.services.InputService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inputs")
@RequiredArgsConstructor
public class InputController {

    private final InputService inputService;

    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<List<InputEntity>>> getInputsByInstrument(@PathVariable Long instrumentId) {
        List<InputEntity> inputs = inputService.findByInstrumentId(instrumentId);
        return ResponseEntity.ok(WebResponseEntity.success(inputs, "Inputs do instrumento obtidos com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<InputEntity>> getInputById(@PathVariable Long id) {
        InputEntity input = inputService.findById(id);
        return ResponseEntity.ok(WebResponseEntity.success(input, "Input obtido com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteInput(@PathVariable Long id) {
        inputService.deleteById(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Input excluído com sucesso!"));
    }
}
