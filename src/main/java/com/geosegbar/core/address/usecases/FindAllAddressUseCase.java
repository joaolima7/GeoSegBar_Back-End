package com.geosegbar.core.address.usecases;

import java.util.List;

import com.geosegbar.adapters.address.AddressRepositoryAdapter;
import com.geosegbar.core.address.entities.AddressEntity;

public class FindAllAddressUseCase {
    private final AddressRepositoryAdapter addressRepositoryAdapter;

    public FindAllAddressUseCase(AddressRepositoryAdapter addressRepositoryAdapter) {
        this.addressRepositoryAdapter = addressRepositoryAdapter;
    }

    public List<AddressEntity> findAll() {
        return addressRepositoryAdapter.findAll();
    }
}
