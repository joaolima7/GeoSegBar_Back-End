package com.geosegbar.core.address.usecases;

import com.geosegbar.adapters.address.AddressRepositoryAdapter;
import com.geosegbar.core.address.entities.AddressEntity;

public class CreateAddresUseCase {
    private final AddressRepositoryAdapter addressRepositoryAdapter;

    public CreateAddresUseCase(AddressRepositoryAdapter addressRepositoryAdapter) {
        this.addressRepositoryAdapter = addressRepositoryAdapter;
    }

    public AddressEntity create(AddressEntity addressEntity) {
        return addressRepositoryAdapter.save(addressEntity);
    }
}
