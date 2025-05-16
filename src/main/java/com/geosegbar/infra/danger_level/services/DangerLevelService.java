package com.geosegbar.infra.danger_level.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.danger_level.persistence.jpa.DangerLevelRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DangerLevelService {

    private final DangerLevelRepository dangerLevelRepository;

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
