package com.geosegbar.core.address.usecases;

import com.geosegbar.adapters.address.AddressRepositoryAdapter;
import com.geosegbar.core.address.entities.AddressEntity;
import com.geosegbar.exceptions.NotFoundException;

public class FindByIdAddressUseCase {
    private final AddressRepositoryAdapter addressRepositoryAdapter;

    public FindByIdAddressUseCase(AddressRepositoryAdapter addressRepositoryAdapter) {
        this.addressRepositoryAdapter = addressRepositoryAdapter;
    }

    public AddressEntity findById(Long id) {
        return addressRepositoryAdapter.findById(id).orElseThrow(() -> new NotFoundException("Endereco não encontrado!"));
    }
}
