package com.geosegbar.infra.sex.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.core.sex.entities.SexEntity;
import com.geosegbar.core.sex.usecases.CreateSexUseCase;
import com.geosegbar.core.sex.usecases.DeleteSexUseCase;
import com.geosegbar.core.sex.usecases.FindAllSexUseCase;
import com.geosegbar.core.sex.usecases.FindByIdSexUseCase;
import com.geosegbar.core.sex.usecases.UpdateSexUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SexService {
    private final FindAllSexUseCase findAllSexUseCase;
    private final FindByIdSexUseCase findByIdSexUseCase;
    private final CreateSexUseCase createSexUseCase;
    private final UpdateSexUseCase updateSexUseCase;
    private final DeleteSexUseCase deleteSexUseCase;

    public SexEntity create(SexEntity sexEntity){
        return createSexUseCase.create(sexEntity);
    }

    public SexEntity update(SexEntity sexEntity){
        return updateSexUseCase.update(sexEntity);
    }

    public void delete(Long id){
        deleteSexUseCase.delete(id);
    }

    public SexEntity findById(Long id){
        return findByIdSexUseCase.findById(id);
    }

    public List<SexEntity> findAll(){
        return findAllSexUseCase.findAll();
    }

}
