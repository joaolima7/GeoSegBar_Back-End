package com.geosegbar.infra.anomaly_status.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.anomaly_status.persistence.jpa.AnomalyStatusRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnomalyStatusService {

    private final AnomalyStatusRepository anomalyStatusRepository;

    public List<AnomalyStatusEntity> findAll() {
        return anomalyStatusRepository.findAll();
    }

    public AnomalyStatusEntity findById(Long id) {
        return anomalyStatusRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Anomaly status not found!"));
    }

    public AnomalyStatusEntity findByName(String name) {
        return anomalyStatusRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Anomaly status not found with name: " + name));
    }
}
