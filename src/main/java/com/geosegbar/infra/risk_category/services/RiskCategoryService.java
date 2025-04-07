package com.geosegbar.infra.risk_category.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.RiskCategoryEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.risk_category.persistence.RiskCategoryRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RiskCategoryService {
    
    private final RiskCategoryRepository riskCategoryRepository;

    @Transactional
    public void deleteById(Long id) {
        riskCategoryRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Categoria de risco não encontrada para exclusão!"));

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
        riskCategoryRepository.findById(riskCategoryEntity.getId())
        .orElseThrow(() -> new NotFoundException("Categoria de risco não encontrada para atualização!"));

        if (riskCategoryRepository.existsByNameAndIdNot(riskCategoryEntity.getName(), riskCategoryEntity.getId())) {
            throw new DuplicateResourceException("Já existe uma categoria de risco com este nome!");
        }
        
        return riskCategoryRepository.save(riskCategoryEntity);
    }

    public RiskCategoryEntity findById(Long id) {
        return riskCategoryRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Categoria de risco não encontrada!"));
    }

    public List<RiskCategoryEntity> findAll() {
        return riskCategoryRepository.findAllByOrderByIdAsc();
    }
}