package com.geosegbar.configs.di;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geosegbar.core.client.usecases.CreateClientUseCase;
import com.geosegbar.core.client.usecases.DeleteClientUseCase;
import com.geosegbar.core.client.usecases.FindAllClientUseCase;
import com.geosegbar.core.client.usecases.FindByIdClientUseCase;
import com.geosegbar.core.client.usecases.UpdateClientUseCase;
import com.geosegbar.infra.client.persistence.jpa.ClientJpaRepositoryImp;

@Configuration
public class ClientConfig {
    
    @Bean
    public CreateClientUseCase createClientUseCase(ClientJpaRepositoryImp adapter) {
        return new CreateClientUseCase(adapter);
    }

    @Bean
    public DeleteClientUseCase deleteClientUseCase(ClientJpaRepositoryImp adapter) {
        return new DeleteClientUseCase(adapter);
    }

    @Bean
    public FindAllClientUseCase findAllClientUseCase(ClientJpaRepositoryImp adapter) {
        return new FindAllClientUseCase(adapter);
    }

    @Bean
    public FindByIdClientUseCase findByIdClientUseCase(ClientJpaRepositoryImp adapter) {
        return new FindByIdClientUseCase(adapter);
    }

    @Bean
    public UpdateClientUseCase updateClientUseCase(ClientJpaRepositoryImp adapter) {
        return new UpdateClientUseCase(adapter);
    }
}
