package com.geosegbar.infra.potential_damage.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.PotentialDamageEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.potential_damage.persistence.PotentialDamageRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PotentialDamageService {

    private final PotentialDamageRepository potentialDamageRepository;

    @PostConstruct
    @Transactional
    public void initializeDefaultPotentialDamages() {
        createIfNotExists("Baixo");
        createIfNotExists("Médio");
        createIfNotExists("Alto");
    }

    private void createIfNotExists(String name) {
        if (!potentialDamageRepository.existsByName(name)) {
            PotentialDamageEntity potentialDamage = new PotentialDamageEntity();
            potentialDamage.setName(name);
            potentialDamageRepository.save(potentialDamage);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        potentialDamageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dano potencial não encontrado para exclusão!"));

        potentialDamageRepository.deleteById(id);
    }

    @Transactional
    public PotentialDamageEntity save(PotentialDamageEntity potentialDamageEntity) {
        if (potentialDamageRepository.existsByName(potentialDamageEntity.getName())) {
            throw new DuplicateResourceException("Já existe um dano potencial com este nome!");
        }

        return potentialDamageRepository.save(potentialDamageEntity);
    }

    @Transactional
    public PotentialDamageEntity update(PotentialDamageEntity potentialDamageEntity) {
        potentialDamageRepository.findById(potentialDamageEntity.getId())
                .orElseThrow(() -> new NotFoundException("Dano potencial não encontrado para atualização!"));

        if (potentialDamageRepository.existsByNameAndIdNot(potentialDamageEntity.getName(), potentialDamageEntity.getId())) {
            throw new DuplicateResourceException("Já existe um dano potencial com este nome!");
        }

        return potentialDamageRepository.save(potentialDamageEntity);
    }

    public PotentialDamageEntity findById(Long id) {
        return potentialDamageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dano potencial não encontrado!"));
    }

    public List<PotentialDamageEntity> findAll() {
        return potentialDamageRepository.findAllByOrderByIdAsc();
    }
}
