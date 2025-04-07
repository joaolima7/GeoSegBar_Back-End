package com.geosegbar.infra.classification_dam.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.ClassificationDamEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.classification_dam.peristence.ClassificationDamRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassificationDamService {
    
    private final ClassificationDamRepository classificationDamRepository;

    @Transactional
    public void deleteById(Long id) {
        classificationDamRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Classificação de barragem não encontrada para exclusão!"));

        classificationDamRepository.deleteById(id);
    }

    @Transactional
    public ClassificationDamEntity save(ClassificationDamEntity classificationDamEntity) {
        if (classificationDamRepository.existsByClassification(classificationDamEntity.getClassification())) {
            throw new DuplicateResourceException("Já existe uma classificação de barragem com este nome!");
        }

        return classificationDamRepository.save(classificationDamEntity);
    }

    @Transactional
    public ClassificationDamEntity update(ClassificationDamEntity classificationDamEntity) {
        classificationDamRepository.findById(classificationDamEntity.getId())
        .orElseThrow(() -> new NotFoundException("Classificação de barragem não encontrada para atualização!"));

        if (classificationDamRepository.existsByClassificationAndIdNot(classificationDamEntity.getClassification(), classificationDamEntity.getId())) {
            throw new DuplicateResourceException("Já existe uma classificação de barragem com este nome!");
        }
        
        return classificationDamRepository.save(classificationDamEntity);
    }

    public ClassificationDamEntity findById(Long id) {
        return classificationDamRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Classificação de barragem não encontrada!"));
    }

    public List<ClassificationDamEntity> findAll() {
        return classificationDamRepository.findAllByOrderByIdAsc();
    }
}