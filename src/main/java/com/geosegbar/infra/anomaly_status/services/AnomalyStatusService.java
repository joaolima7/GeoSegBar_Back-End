package com.geosegbar.infra.anomaly_status.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.anomaly_status.persistence.jpa.AnomalyStatusRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnomalyStatusService {

    private final AnomalyStatusRepository anomalyStatusRepository;

    @PostConstruct
    @Transactional
    public void initializeDefaultStatus() {
        createIfNotExists("Pendente", "Anomalia identificada, aguardando análise");
        createIfNotExists("Em andamento", "Anomalia em processo de tratamento");
        createIfNotExists("Concluído", "Tratamento da anomalia finalizado");
        createIfNotExists("Em monitoramento", "Anomalia tratada mas sob observação");
    }

    private void createIfNotExists(String name, String description) {
        Optional<AnomalyStatusEntity> existingStatus = anomalyStatusRepository.findByName(name);

        if (existingStatus.isEmpty()) {
            AnomalyStatusEntity status = new AnomalyStatusEntity();
            status.setName(name);
            status.setDescription(description);
            anomalyStatusRepository.save(status);
        }
    }

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
