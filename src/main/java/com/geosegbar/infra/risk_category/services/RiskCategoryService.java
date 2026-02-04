package com.geosegbar.infra.risk_category.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.RiskCategoryEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.risk_category.persistence.RiskCategoryRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskCategoryService {

    private final RiskCategoryRepository riskCategoryRepository;

    @PostConstruct
    @Transactional
    public void initializeDefaultRiskCategories() {
        createIfNotExists("Baixo");
        createIfNotExists("Médio");
        createIfNotExists("Alto");
    }

    private void createIfNotExists(String name) {
        if (!riskCategoryRepository.existsByName(name)) {
            RiskCategoryEntity riskCategory = new RiskCategoryEntity();
            riskCategory.setName(name);
            riskCategoryRepository.save(riskCategory);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        if (!riskCategoryRepository.existsById(id)) {
            throw new NotFoundException("Categoria de risco não encontrada para exclusão!");
        }
        riskCategoryRepository.deleteById(id);
    }

    @Transactional
    public RiskCategoryEntity save(RiskCategoryEntity riskCategoryEntity) {
        if (riskCategoryRepository.existsByName(riskCategoryEntity.getName())) {
            throw new DuplicateResourceException("Já existe uma categoria de risco com este nome!");
        }

        return riskCategoryRepository.save(riskCategoryEntity);
    }

    @Transactional
    public RiskCategoryEntity update(RiskCategoryEntity riskCategoryEntity) {

        if (!riskCategoryRepository.existsById(riskCategoryEntity.getId())) {
            throw new NotFoundException("Categoria de risco não encontrada para atualização!");
        }

        if (riskCategoryRepository.existsByNameAndIdNot(riskCategoryEntity.getName(), riskCategoryEntity.getId())) {
            throw new DuplicateResourceException("Já existe uma categoria de risco com este nome!");
        }

        return riskCategoryRepository.save(riskCategoryEntity);
    }

    @Transactional(readOnly = true)
    public RiskCategoryEntity findById(Long id) {
        return riskCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria de risco não encontrada!"));
    }

    @Transactional(readOnly = true)
    public List<RiskCategoryEntity> findAll() {
        return riskCategoryRepository.findAllByOrderByIdAsc();
    }
}
