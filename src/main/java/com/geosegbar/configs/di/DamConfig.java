package com.geosegbar.configs.di;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geosegbar.core.dam.usecases.CreateDamUseCase;
import com.geosegbar.core.dam.usecases.DeleteDamUseCase;
import com.geosegbar.core.dam.usecases.FindAllDamUseCase;
import com.geosegbar.core.dam.usecases.FindByIdDamUseCase;
import com.geosegbar.core.dam.usecases.UpdateDamUseCase;
import com.geosegbar.infra.dam.persistence.jpa.DamJpaRepositoryImp;

@Configuration
public class DamConfig {
    
    @Bean
    public CreateDamUseCase createDamUseCase(DamJpaRepositoryImp adapter) {
        return new CreateDamUseCase(adapter);
    }

    @Bean
    public DeleteDamUseCase deleteDamUseCase(DamJpaRepositoryImp adapter) {
        return new DeleteDamUseCase(adapter);
    }

    @Bean
    public FindAllDamUseCase findAllDamUseCase(DamJpaRepositoryImp adapter) {
        return new FindAllDamUseCase(adapter);
    }

    @Bean
    public FindByIdDamUseCase findByIdDamUseCase(DamJpaRepositoryImp adapter) {
        return new FindByIdDamUseCase(adapter);
    }

    @Bean
    public UpdateDamUseCase updateDamUseCase(DamJpaRepositoryImp adapter) {
        return new UpdateDamUseCase(adapter);
    }
}
