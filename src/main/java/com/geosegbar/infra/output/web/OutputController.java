package com.geosegbar.infra.output.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.infra.output.services.OutputService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/outputs")
@RequiredArgsConstructor
public class OutputController {

    private final OutputService outputService;

    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<WebResponseEntity<List<OutputEntity>>> getOutputsByInstrument(@PathVariable Long instrumentId) {
        List<OutputEntity> outputs = outputService.findByInstrumentId(instrumentId);
        return ResponseEntity.ok(WebResponseEntity.success(outputs, "Outputs do instrumento obtidos com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<OutputEntity>> getOutputById(@PathVariable Long id) {
        OutputEntity output = outputService.findById(id);
        return ResponseEntity.ok(WebResponseEntity.success(output, "Output obtido com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteOutput(@PathVariable Long id) {
        outputService.deleteById(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Output excluído com sucesso!"));
    }
}
