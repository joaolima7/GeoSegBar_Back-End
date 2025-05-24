package com.geosegbar.infra.danger_level.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.danger_level.persistence.jpa.DangerLevelRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DangerLevelService {

    private final DangerLevelRepository dangerLevelRepository;

    @PostConstruct
    @Transactional
    public void initializeDefaultDangerLevels() {
        createIfNotExists("0 - Normal", "Condições normais de operação");
        createIfNotExists("1 - Atenção", "Anomalia que requer atenção e verificação periódica");
        createIfNotExists("2 - Alerta", "Anomalia com risco potencial que requer intervenção");
        createIfNotExists("3 - Emergência", "Anomalia crítica que requer ação imediata");
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

    public List<DangerLevelEntity> findAll() {
        return dangerLevelRepository.findAll();
    }

    public DangerLevelEntity findById(Long id) {
        return dangerLevelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Danger level not found!"));
    }

    public DangerLevelEntity findByName(String name) {
        return dangerLevelRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Danger level not found with name: " + name));
    }
}
