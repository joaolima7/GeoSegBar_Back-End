package com.geosegbar.infra.status.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.StatusEnum;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final StatusRepository statusRepository;

    @PostConstruct
    public void initializeStatuses() {
        if (statusRepository.findByStatus(StatusEnum.ACTIVE).isEmpty()) {
            StatusEntity activeStatus = new StatusEntity();
            activeStatus.setStatus(StatusEnum.ACTIVE);
            statusRepository.save(activeStatus);
        }
        
        if (statusRepository.findByStatus(StatusEnum.DISABLED).isEmpty()) {
            StatusEntity disabledStatus = new StatusEntity();
            disabledStatus.setStatus(StatusEnum.DISABLED);
            statusRepository.save(disabledStatus);
        }
    }

    public StatusEntity getActiveStatus() {
        return statusRepository.findByStatus(StatusEnum.ACTIVE)
            .orElseThrow(() -> new NotFoundException("Status ATIVO não encontrado!"));
    }
    
    public StatusEntity getDisabledStatus() {
        return statusRepository.findByStatus(StatusEnum.DISABLED)
            .orElseThrow(() -> new NotFoundException("Status DESATIVADO não encontrado!"));
    }
    
    public StatusEntity findById(Long id) {
        return statusRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Status não encontrado!"));
    }
    
    public List<StatusEntity> findAll() {
        return statusRepository.findAll();
    }
    
    @Transactional
    public StatusEntity update(StatusEntity status) {
        statusRepository.findById(status.getId())
            .orElseThrow(() -> new NotFoundException("Status não encontrado!"));
        
        return statusRepository.save(status);
    }
}