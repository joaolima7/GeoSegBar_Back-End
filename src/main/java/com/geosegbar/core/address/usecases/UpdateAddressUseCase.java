package com.geosegbar.core.address.usecases;

import com.geosegbar.adapters.address.AddressRepositoryAdapter;
import com.geosegbar.core.address.entities.AddressEntity;

public class UpdateAddressUseCase {
        private final AddressRepositoryAdapter addressRepositoryAdapter;

        public UpdateAddressUseCase(AddressRepositoryAdapter addressRepositoryAdapter) {
            this.addressRepositoryAdapter = addressRepositoryAdapter;
        }

        public AddressEntity update(AddressEntity entity) {
            return addressRepositoryAdapter.update(entity);
        }
}
