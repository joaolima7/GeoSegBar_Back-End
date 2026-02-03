package com.geosegbar.infra.danger_level.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.danger_level.persistence.jpa.DangerLevelRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DangerLevelService {

    private final DangerLevelRepository dangerLevelRepository;

    @PostConstruct
    @Transactional
    public void initializeDefaultDangerLevels() {
        createIfNotExists("Normal", "Condições normais de operação");
        createIfNotExists("Atenção", "Anomalia que requer atenção e verificação periódica");
        createIfNotExists("Alerta", "Anomalia com risco potencial que requer intervenção");
        createIfNotExists("Emergência", "Anomalia crítica que requer ação imediata");
        createIfNotExists("--", "Ainda não foi definido um nível de perigo");
    }

    private void createIfNotExists(String name, String description) {
        Optional<DangerLevelEntity> existingLevel = dangerLevelRepository.findByName(name);

        if (existingLevel.isEmpty()) {
            DangerLevelEntity dangerLevel = new DangerLevelEntity();
            dangerLevel.setName(name);
            dangerLevel.setDescription(description);
            dangerLevelRepository.save(dangerLevel);
        }
    }

    @Transactional(readOnly = true)
    public List<DangerLevelEntity> findAll() {
        return dangerLevelRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DangerLevelEntity findById(Long id) {
        return dangerLevelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Danger level not found!"));
    }

    @Transactional(readOnly = true)
    public DangerLevelEntity findByName(String name) {
        return dangerLevelRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Danger level not found with name: " + name));
    }
}
