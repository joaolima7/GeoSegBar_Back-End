package com.geosegbar.infra.checklist_response.web;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.infra.checklist_response.dtos.ChecklistResponseDetailDTO;
import com.geosegbar.infra.checklist_response.dtos.PagedChecklistResponseDTO;
import com.geosegbar.infra.checklist_response.services.ChecklistResponseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/checklist-responses")
@RequiredArgsConstructor
public class ChecklistResponseController {

    private final ChecklistResponseService checklistResponseService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<ChecklistResponseEntity>>> getAllChecklistResponses() {
        List<ChecklistResponseEntity> responses = checklistResponseService.findAll();
        WebResponseEntity<List<ChecklistResponseEntity>> response = WebResponseEntity.success(responses, "Respostas de checklist obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ChecklistResponseEntity>> getChecklistResponseById(@PathVariable Long id) {
        ChecklistResponseEntity checklistResponse = checklistResponseService.findById(id);
        WebResponseEntity<ChecklistResponseEntity> response = WebResponseEntity.success(checklistResponse, "Resposta de checklist obtida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dam/{damId}")
    public ResponseEntity<WebResponseEntity<List<ChecklistResponseEntity>>> getChecklistResponsesByDamId(@PathVariable Long damId) {
        List<ChecklistResponseEntity> responses = checklistResponseService.findByDamId(damId);
        WebResponseEntity<List<ChecklistResponseEntity>> response = WebResponseEntity.success(responses, "Respostas de checklist da barragem obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<ChecklistResponseEntity>> createChecklistResponse(@Valid @RequestBody ChecklistResponseEntity checklistResponse) {
        ChecklistResponseEntity created = checklistResponseService.save(checklistResponse);
        WebResponseEntity<ChecklistResponseEntity> response = WebResponseEntity.success(created, "Resposta de checklist criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ChecklistResponseEntity>> updateChecklistResponse(@PathVariable Long id, @Valid @RequestBody ChecklistResponseEntity checklistResponse) {
        checklistResponse.setId(id);
        ChecklistResponseEntity updated = checklistResponseService.update(checklistResponse);
        WebResponseEntity<ChecklistResponseEntity> response = WebResponseEntity.success(updated, "Resposta de checklist atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteChecklistResponse(@PathVariable Long id) {
        checklistResponseService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Resposta de checklist excluída com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dam/{damId}/detail")
    public ResponseEntity<WebResponseEntity<List<ChecklistResponseDetailDTO>>> getDetailedDamChecklistResponses(@PathVariable Long damId) {
        List<ChecklistResponseDetailDTO> responses = checklistResponseService.findChecklistResponsesByDamId(damId);
        
        WebResponseEntity<List<ChecklistResponseDetailDTO>> response = WebResponseEntity.success(
                responses, 
                "Respostas de checklist da barragem obtidas com sucesso!"
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}/detail")
    public ResponseEntity<WebResponseEntity<ChecklistResponseDetailDTO>> getDetailedChecklistResponse(@PathVariable Long id) {
        ChecklistResponseDetailDTO response = checklistResponseService.findChecklistResponseById(id);
        
        WebResponseEntity<ChecklistResponseDetailDTO> webResponse = WebResponseEntity.success(
                response, 
                "Detalhes da resposta de checklist obtidos com sucesso!"
        );
        
        return ResponseEntity.ok(webResponse);
    }

    @GetMapping("/user/{userId}/detail")
    public ResponseEntity<WebResponseEntity<List<ChecklistResponseDetailDTO>>> getDetailedUserChecklistResponses(@PathVariable Long userId) {
    List<ChecklistResponseDetailDTO> responses = checklistResponseService.findChecklistResponsesByUserId(userId);
    
    WebResponseEntity<List<ChecklistResponseDetailDTO>> response = WebResponseEntity.success(
            responses, 
            "Respostas de checklist do usuário obtidas com sucesso!"
    );
    
    return ResponseEntity.ok(response);
    }

    @GetMapping("/date-range/detail")
    public ResponseEntity<WebResponseEntity<List<ChecklistResponseDetailDTO>>> getDetailedChecklistResponsesByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
    
    List<ChecklistResponseDetailDTO> responses = checklistResponseService.findChecklistResponsesByDateRange(startDate, endDate);
    
    WebResponseEntity<List<ChecklistResponseDetailDTO>> response = WebResponseEntity.success(
            responses, 
            "Respostas de checklist no período especificado obtidas com sucesso!"
    );
    
    return ResponseEntity.ok(response);
    }

    @GetMapping("/paged")
    public ResponseEntity<WebResponseEntity<PagedChecklistResponseDTO<ChecklistResponseDetailDTO>>> getAllChecklistResponsesPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        PagedChecklistResponseDTO<ChecklistResponseDetailDTO> responses = 
                checklistResponseService.findAllChecklistResponsesPaged(pageable);
        
        WebResponseEntity<PagedChecklistResponseDTO<ChecklistResponseDetailDTO>> response = WebResponseEntity.success(
                responses, 
                "Respostas de checklist paginadas obtidas com sucesso!"
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dam/{damId}/paged")
    public ResponseEntity<WebResponseEntity<PagedChecklistResponseDTO<ChecklistResponseDetailDTO>>> getDetailedDamChecklistResponsesPaged(
            @PathVariable Long damId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        PagedChecklistResponseDTO<ChecklistResponseDetailDTO> responses = 
                checklistResponseService.findChecklistResponsesByDamIdPaged(damId, pageable);
        
        WebResponseEntity<PagedChecklistResponseDTO<ChecklistResponseDetailDTO>> response = WebResponseEntity.success(
                responses, 
                "Respostas de checklist da barragem paginadas obtidas com sucesso!"
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<WebResponseEntity<PagedChecklistResponseDTO<ChecklistResponseDetailDTO>>> getDetailedUserChecklistResponsesPaged(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        PagedChecklistResponseDTO<ChecklistResponseDetailDTO> responses = 
                checklistResponseService.findChecklistResponsesByUserIdPaged(userId, pageable);
        
        WebResponseEntity<PagedChecklistResponseDTO<ChecklistResponseDetailDTO>> response = WebResponseEntity.success(
                responses, 
                "Respostas de checklist do usuário paginadas obtidas com sucesso!"
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date-range/paged")
    public ResponseEntity<WebResponseEntity<PagedChecklistResponseDTO<ChecklistResponseDetailDTO>>> getDetailedChecklistResponsesByDateRangePaged(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        PagedChecklistResponseDTO<ChecklistResponseDetailDTO> responses = 
                checklistResponseService.findChecklistResponsesByDateRangePaged(startDate, endDate, pageable);
        
        WebResponseEntity<PagedChecklistResponseDTO<ChecklistResponseDetailDTO>> response = WebResponseEntity.success(
                responses, 
                "Respostas de checklist no período especificado paginadas obtidas com sucesso!"
        );
        
        return ResponseEntity.ok(response);
    }
}