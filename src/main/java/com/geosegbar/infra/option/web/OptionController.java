package com.geosegbar.infra.option.web;

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
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.infra.option.services.OptionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/options")
@RequiredArgsConstructor
public class OptionController {

    private final OptionService optionService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<OptionEntity>>> getAllOptions() {
        List<OptionEntity> options = optionService.findAll();
        WebResponseEntity<List<OptionEntity>> response = WebResponseEntity.success(options, "Opções obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<OptionEntity>> getOptionById(@PathVariable Long id) {
        OptionEntity option = optionService.findById(id);
        WebResponseEntity<OptionEntity> response = WebResponseEntity.success(option, "Opção obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<OptionEntity>> createOption(@Valid @RequestBody OptionEntity option) {
        OptionEntity created = optionService.save(option);
        WebResponseEntity<OptionEntity> response = WebResponseEntity.success(created, "Opção criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<OptionEntity>> updateOption(@PathVariable Long id, @Valid @RequestBody OptionEntity option) {
        option.setId(id);
        OptionEntity updated = optionService.update(option);
        WebResponseEntity<OptionEntity> response = WebResponseEntity.success(updated, "Opção atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteOption(@PathVariable Long id) {
        optionService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Opção excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}
