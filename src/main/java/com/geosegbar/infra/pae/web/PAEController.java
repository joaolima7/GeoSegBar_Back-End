package com.geosegbar.infra.pae.web;

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
import com.geosegbar.infra.pae.dtos.PAEDTO;
import com.geosegbar.infra.pae.dtos.PAEResponseDTO;
import com.geosegbar.infra.pae.services.PAEService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pae")
@RequiredArgsConstructor
public class PAEController {

    private final PAEService paeService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<PAEResponseDTO>>> getAllPAEs() {
        List<PAEResponseDTO> paeList = paeService.findAll();
        return ResponseEntity.ok(WebResponseEntity.success(paeList, "PAEs obtidos com sucesso!"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<PAEResponseDTO>> getPAEById(@PathVariable Long id) {
        PAEResponseDTO pae = paeService.findById(id);
        return ResponseEntity.ok(WebResponseEntity.success(pae, "PAE obtido com sucesso!"));
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<PAEResponseDTO>> getPAEByDamId(@PathVariable Long damId) {
        PAEResponseDTO pae = paeService.findByDamId(damId);
        return ResponseEntity.ok(WebResponseEntity.success(pae, "PAE obtido com sucesso!"));
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<PAEResponseDTO>> createPAE(@Valid @RequestBody PAEDTO dto) {
        PAEResponseDTO created = paeService.createOrUpdate(dto);
        return new ResponseEntity<>(WebResponseEntity.success(created, "PAE criado com sucesso!"), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<PAEResponseDTO>> updatePAE(
            @PathVariable Long id,
            @Valid @RequestBody PAEDTO dto) {
        dto.setId(id);
        PAEResponseDTO updated = paeService.createOrUpdate(dto);
        return ResponseEntity.ok(WebResponseEntity.success(updated, "PAE atualizado com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deletePAE(@PathVariable Long id) {
        paeService.delete(id);
        return ResponseEntity.ok(WebResponseEntity.success(null, "PAE excluído com sucesso!"));
    }
}
