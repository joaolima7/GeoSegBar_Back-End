package com.geosegbar.infra.instrument.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.infra.instrument.dtos.CreateInstrumentRequest;
import com.geosegbar.infra.instrument.dtos.InstrumentResponseDTO;
import com.geosegbar.infra.instrument.dtos.UpdateInstrumentRequest;
import com.geosegbar.infra.instrument.services.InstrumentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService instrumentService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<InstrumentResponseDTO>>> getAllInstruments() {
        List<InstrumentEntity> instruments = instrumentService.findAll();
        List<InstrumentResponseDTO> dtos = instrumentService.mapToResponseDTOList(instruments);
        return ResponseEntity.ok(WebResponseEntity.success(dtos, "Instrumentos obtidos com sucesso!"));
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<InstrumentResponseDTO>>> getInstrumentsByDam(@PathVariable Long damId) {
        List<InstrumentEntity> instruments = instrumentService.findByDamId(damId);
        List<InstrumentResponseDTO> dtos = instrumentService.mapToResponseDTOList(instruments);
        return ResponseEntity.ok(WebResponseEntity.success(dtos, "Instrumentos da barragem obtidos com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<InstrumentResponseDTO>> getInstrumentById(@PathVariable Long id) {
        InstrumentEntity instrument = instrumentService.findWithAllDetails(id);
        InstrumentResponseDTO dto = instrumentService.mapToResponseDTO(instrument);
        return ResponseEntity.ok(WebResponseEntity.success(dto, "Instrumento obtido com sucesso!"));
    }

    @GetMapping("/filter")
    public ResponseEntity<WebResponseEntity<List<InstrumentResponseDTO>>> filterInstruments(
            @RequestParam(required = false) Long damId,
            @RequestParam(required = false) Long instrumentTypeId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long clientId) {

        List<InstrumentEntity> instruments = instrumentService.findByFilters(damId, instrumentTypeId, sectionId, active, clientId);
        List<InstrumentResponseDTO> responseList = instrumentService.mapToResponseDTOList(instruments);

        return ResponseEntity.ok(WebResponseEntity.success(responseList, "Instrumentos obtidos com sucesso!"));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<WebResponseEntity<List<InstrumentResponseDTO>>> getInstrumentsByClient(
            @PathVariable Long clientId,
            @RequestParam(required = false) Boolean active) {
        List<InstrumentEntity> instruments = instrumentService.findByClientId(clientId, active);
        List<InstrumentResponseDTO> dtos = instrumentService.mapToResponseDTOList(instruments);

        return ResponseEntity.ok(WebResponseEntity.success(
                dtos,
                "Instrumentos do cliente obtidos com sucesso!"
        ));
    }

    @PatchMapping("/{id}/{active}")
    public ResponseEntity<WebResponseEntity<InstrumentResponseDTO>> activateInstrument(@PathVariable Long id, @PathVariable Boolean active) {
        InstrumentEntity instrument = instrumentService.toggleActiveInstrument(id, active);
        InstrumentResponseDTO response = instrumentService.mapToResponseDTO(instrument);
        String action = active ? "ativado" : "desativado";
        return ResponseEntity.ok(WebResponseEntity.success(response, "Instrumento " + action + " com sucesso!"));
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<InstrumentResponseDTO>> createInstrument(@Valid @RequestBody CreateInstrumentRequest request) {
        InstrumentEntity createdInstrument = instrumentService.createComplete(request);
        InstrumentResponseDTO dto = instrumentService.mapToResponseDTO(createdInstrument);
        return new ResponseEntity<>(WebResponseEntity.success(dto, "Instrumento criado com sucesso!"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<InstrumentResponseDTO>> update(@PathVariable Long id, @Valid @RequestBody UpdateInstrumentRequest request) {
        InstrumentEntity updated = instrumentService.update(id, request);
        return ResponseEntity.ok(WebResponseEntity.success(instrumentService.mapToResponseDTO(updated), "Instrumento atualizado com sucesso!"));
    }

    @PatchMapping("/{id}/section-visibility/{active}")
    public ResponseEntity<WebResponseEntity<InstrumentResponseDTO>> toggleSectionVisibility(
            @PathVariable Long id,
            @PathVariable Boolean active) {
        InstrumentEntity instrument = instrumentService.toggleSectionVisibility(id, active);
        InstrumentResponseDTO response = instrumentService.mapToResponseDTO(instrument);
        String action = active ? "visível" : "oculto";
        return ResponseEntity.ok(WebResponseEntity.success(
                response,
                "Instrumento " + action + " na aba de seções com sucesso!"
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteInstrument(@PathVariable Long id) {
        instrumentService.delete(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "Instrumento excluído com sucesso!"));
    }
}
