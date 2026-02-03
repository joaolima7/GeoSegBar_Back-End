package com.geosegbar.infra.classification_dam.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.ClassificationDamEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.classification_dam.persistence.ClassificationDamRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassificationDamService {

    private final ClassificationDamRepository classificationDamRepository;

    @PostConstruct
    @Transactional
    public void initializeDefaultClassifications() {
        createIfNotExists("A");
        createIfNotExists("B");
        createIfNotExists("C");
        createIfNotExists("D");
        createIfNotExists("E");
    }

    private void createIfNotExists(String classification) {
        if (!classificationDamRepository.existsByClassification(classification)) {
            ClassificationDamEntity classificationDam = new ClassificationDamEntity();
            classificationDam.setClassification(classification);
            classificationDamRepository.save(classificationDam);
        }
    }

    @Transactional
    public void deleteById(Long id) {

        if (!classificationDamRepository.existsById(id)) {
            throw new NotFoundException("Classificação de barragem não encontrada para exclusão!");
        }
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
        if (!classificationDamRepository.existsById(classificationDamEntity.getId())) {
            throw new NotFoundException("Classificação de barragem não encontrada para atualização!");
        }

        if (classificationDamRepository.existsByClassificationAndIdNot(classificationDamEntity.getClassification(), classificationDamEntity.getId())) {
            throw new DuplicateResourceException("Já existe uma classificação de barragem com este nome!");
        }

        return classificationDamRepository.save(classificationDamEntity);
    }

    @Transactional(readOnly = true)
    public ClassificationDamEntity findById(Long id) {
        return classificationDamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Classificação de barragem não encontrada!"));
    }

    @Transactional(readOnly = true)
    public List<ClassificationDamEntity> findAll() {
        return classificationDamRepository.findAllByOrderByIdAsc();
    }
}
