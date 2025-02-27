package com.geosegbar.infra.dam.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.core.dam.entities.DamEntity;
import com.geosegbar.core.dam.usecases.CreateDamUseCase;
import com.geosegbar.core.dam.usecases.DeleteDamUseCase;
import com.geosegbar.core.dam.usecases.FindAllDamUseCase;
import com.geosegbar.core.dam.usecases.FindByIdDamUseCase;
import com.geosegbar.core.dam.usecases.UpdateDamUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DamService {
    
    private final FindAllDamUseCase findAllDamUseCase;
    private final FindByIdDamUseCase findByIdDamUseCase;
    private final CreateDamUseCase createDamUseCase;
    private final UpdateDamUseCase updateDamUseCase;
    private final DeleteDamUseCase deleteDamUseCase;

    public DamEntity create(DamEntity dam) {
        return createDamUseCase.create(dam);
    }

    public DamEntity update(DamEntity dam) {
        return updateDamUseCase.update(dam);
    }

    public void delete(Long id) {
         deleteDamUseCase.delete(id);
    }

    public DamEntity findById(Long id) {
         return findByIdDamUseCase.findById(id);
    }

    public List<DamEntity> findAll() {
         return findAllDamUseCase.findAll();
    }
}
