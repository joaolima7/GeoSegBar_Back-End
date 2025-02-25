package com.geosegbar.configs.di;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geosegbar.core.address.usecases.CreateAddresUseCase;
import com.geosegbar.core.address.usecases.DeleteAddressUseCase;
import com.geosegbar.core.address.usecases.FindAllAddressUseCase;
import com.geosegbar.core.address.usecases.FindByIdAddressUseCase;
import com.geosegbar.core.address.usecases.UpdateAddressUseCase;
import com.geosegbar.infra.address.persistence.jpa.AddressJpaRespositoryImp;

@Configuration
public class AddressConfig {
    @Bean
    public CreateAddresUseCase createAddresUseCase(AddressJpaRespositoryImp adapter) {
    return new CreateAddresUseCase(adapter);
    }

    @Bean
        public DeleteAddressUseCase deleteAddressUseCase(AddressJpaRespositoryImp adapter) {
        return new DeleteAddressUseCase(adapter);
    }

        @Bean
    public FindAllAddressUseCase findAllAddressUseCase(AddressJpaRespositoryImp adapter) {
        return new FindAllAddressUseCase(adapter);
    }

    @Bean
    public FindByIdAddressUseCase findByIdAddressUseCase(AddressJpaRespositoryImp adapter) {
        return new FindByIdAddressUseCase(adapter);
    }

    @Bean
    public UpdateAddressUseCase updateAddressUseCase(AddressJpaRespositoryImp adapter) {
        return new UpdateAddressUseCase(adapter);
    }
}
