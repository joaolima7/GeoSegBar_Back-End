package com.geosegbar.infra.checklist.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistRepository checklistRepository;

    public List<ChecklistEntity> findAll() {
        return checklistRepository.findAll();
    }

    public ChecklistEntity findById(Long id) {
        return checklistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrada para id: " + id));
    }

    @Transactional
    public ChecklistEntity save(ChecklistEntity checklist) {
        if (checklistRepository.existsByName(checklist.getName())) {
            throw new DuplicateResourceException("Já existe um checklist com este nome!");
        }
        return checklistRepository.save(checklist);
    }

    @Transactional
    public ChecklistEntity update(ChecklistEntity checklist) {
        checklistRepository.findById(checklist.getId())
                .orElseThrow(() -> new NotFoundException("Checklist não encontrada para atualização!"));

        if (checklistRepository.existsByNameAndIdNot(checklist.getName(), checklist.getId())) {
            throw new DuplicateResourceException("Já existe um checklist com este nome!");
        }

        return checklistRepository.save(checklist);
    }

    @Transactional
    public void deleteById(Long id) {
        checklistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrada para exclusão!"));
        checklistRepository.deleteById(id);
    }

    public List<ChecklistEntity> findByDamId(Long damId) {
        List<ChecklistEntity> checklists = checklistRepository.findByDams_Id(damId);
        if (checklists.isEmpty()) {
            throw new NotFoundException("Nenhum checklist encontrado para a Barragem com id: " + damId);
        }
        return checklists;
    }

}
