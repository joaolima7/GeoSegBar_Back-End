package com.geosegbar.core.address.usecases;

import com.geosegbar.adapters.address.AddressRepositoryAdapter;
import com.geosegbar.core.address.entities.AddressEntity;
import com.geosegbar.exceptions.NotFoundException;

public class UpdateAddressUseCase {
        private final AddressRepositoryAdapter addressRepositoryAdapter;

        public UpdateAddressUseCase(AddressRepositoryAdapter addressRepositoryAdapter) {
            this.addressRepositoryAdapter = addressRepositoryAdapter;
        }

        public AddressEntity update(AddressEntity entity) {
            addressRepositoryAdapter.findById(entity.getId())
            .orElseThrow(() -> new NotFoundException("Endereço não encontrado para atualização!"));
            return addressRepositoryAdapter.update(entity);
        }
}
