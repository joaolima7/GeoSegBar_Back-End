package com.geosegbar.infra.instrument.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.infra.instrument.dtos.CreateInstrumentRequest;
import com.geosegbar.infra.instrument.dtos.InstrumentResponseDTO;
import com.geosegbar.infra.instrument.services.InstrumentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService instrumentService;

    @GetMapping
    public ResponseEntity<List<InstrumentResponseDTO>> getAllInstruments() {
        List<InstrumentEntity> instruments = instrumentService.findAll();
        return ResponseEntity.ok(instrumentService.mapToResponseDTOList(instruments));
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<List<InstrumentResponseDTO>> getInstrumentsByDam(@PathVariable Long damId) {
        List<InstrumentEntity> instruments = instrumentService.findByDamId(damId);
        return ResponseEntity.ok(instrumentService.mapToResponseDTOList(instruments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstrumentResponseDTO> getInstrumentById(@PathVariable Long id) {
        InstrumentEntity instrument = instrumentService.findWithAllDetails(id);
        return ResponseEntity.ok(instrumentService.mapToResponseDTO(instrument));
    }

    @PostMapping
    public ResponseEntity<InstrumentResponseDTO> createInstrument(@Valid @RequestBody CreateInstrumentRequest request) {
        InstrumentEntity createdInstrument = instrumentService.createComplete(request);
        return new ResponseEntity<>(instrumentService.mapToResponseDTO(createdInstrument), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstrument(@PathVariable Long id) {
        instrumentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
