package com.geosegbar.infra.status.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.StatusEnum;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final StatusRepository statusRepository;

    @PostConstruct
    @Transactional
    public void initializeStatuses() {
        createIfNotExists(StatusEnum.ACTIVE);
        createIfNotExists(StatusEnum.DISABLED);
    }

    private void createIfNotExists(StatusEnum statusEnum) {
        if (statusRepository.findByStatus(statusEnum).isEmpty()) {
            StatusEntity status = new StatusEntity();
            status.setStatus(statusEnum);
            statusRepository.save(status);
        }
    }

    @Transactional(readOnly = true)
    public StatusEntity getActiveStatus() {
        return statusRepository.findByStatus(StatusEnum.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Status ATIVO não encontrado!"));
    }

    @Transactional(readOnly = true)
    public StatusEntity getDisabledStatus() {
        return statusRepository.findByStatus(StatusEnum.DISABLED)
                .orElseThrow(() -> new NotFoundException("Status DESATIVADO não encontrado!"));
    }

    @Transactional(readOnly = true)
    public StatusEntity findById(Long id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Status não encontrado!"));
    }

    @Transactional(readOnly = true)
    public List<StatusEntity> findAll() {
        return statusRepository.findAll();
    }

    @Transactional
    public StatusEntity update(StatusEntity status) {
        if (!statusRepository.existsById(status.getId())) {
            throw new NotFoundException("Status não encontrado!");
        }
        return statusRepository.save(status);
    }
}
