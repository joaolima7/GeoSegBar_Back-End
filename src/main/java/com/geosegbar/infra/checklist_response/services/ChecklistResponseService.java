package com.geosegbar.infra.checklist_response.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.dam.services.DamService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChecklistResponseService {

    private final ChecklistResponseRepository checklistResponseRepository;
    private final DamService damService;

    public List<ChecklistResponseEntity> findAll() {
        return checklistResponseRepository.findAll();
    }

    public ChecklistResponseEntity findById(Long id) {
        return checklistResponseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para id: " + id));
    }

    public List<ChecklistResponseEntity> findByDamId(Long damId) {
        damService.findById(damId);
        List<ChecklistResponseEntity> responses = checklistResponseRepository.findByDamId(damId);
        if (responses.isEmpty()) {
            throw new NotFoundException("Nenhuma resposta de checklist encontrada para a Barragem com id: " + damId);
        }
        return responses;
    }

    @Transactional
    public ChecklistResponseEntity save(ChecklistResponseEntity checklistResponse) {
        Long damId = checklistResponse.getDam().getId();
        DamEntity dam = damService.findById(damId);
        checklistResponse.setDam(dam);
        
        return checklistResponseRepository.save(checklistResponse);
    }

    @Transactional
    public ChecklistResponseEntity update(ChecklistResponseEntity checklistResponse) {
        checklistResponseRepository.findById(checklistResponse.getId())
            .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para atualização!"));
        
        Long damId = checklistResponse.getDam().getId();
        DamEntity dam = damService.findById(damId);
        checklistResponse.setDam(dam);
        
        return checklistResponseRepository.save(checklistResponse);
    }

    @Transactional
    public void deleteById(Long id) {
        checklistResponseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para exclusão!"));
        checklistResponseRepository.deleteById(id);
    }
}