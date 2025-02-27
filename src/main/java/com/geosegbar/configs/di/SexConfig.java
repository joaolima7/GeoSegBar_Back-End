package com.geosegbar.configs.di;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geosegbar.core.sex.usecases.CreateSexUseCase;
import com.geosegbar.core.sex.usecases.DeleteSexUseCase;
import com.geosegbar.core.sex.usecases.FindAllSexUseCase;
import com.geosegbar.core.sex.usecases.FindByIdSexUseCase;
import com.geosegbar.core.sex.usecases.UpdateSexUseCase;
import com.geosegbar.infra.sex.persistence.jpa.SexJpaRepositoryImp;

@Configuration
public class SexConfig {
    
    @Bean
    public CreateSexUseCase createSexUseCase(SexJpaRepositoryImp adapter) {
        return new CreateSexUseCase(adapter);
    }

    @Bean
    public DeleteSexUseCase deleteSexUseCase(SexJpaRepositoryImp adapter) {
        return new DeleteSexUseCase(adapter);
    }

    @Bean
    public FindAllSexUseCase findAllSexUseCase(SexJpaRepositoryImp adapter) {
        return new FindAllSexUseCase(adapter);
    }

    @Bean
    public FindByIdSexUseCase findByIdSexUseCase(SexJpaRepositoryImp adapter) {
        return new FindByIdSexUseCase(adapter);
    }

    @Bean
    public UpdateSexUseCase updateSexUseCase(SexJpaRepositoryImp adapter) {
        return new UpdateSexUseCase(adapter);
    }
    
}
