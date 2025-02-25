package com.geosegbar.core.address.usecases;

import com.geosegbar.adapters.address.AddressRepositoryAdapter;
import com.geosegbar.exceptions.NotFoundException;

public class DeleteAddressUseCase {
        private final AddressRepositoryAdapter addressRepositoryAdapter;

        public DeleteAddressUseCase(AddressRepositoryAdapter addressRepositoryAdapter) {
            this.addressRepositoryAdapter = addressRepositoryAdapter;
        }

        public void delete(Long id) {
            addressRepositoryAdapter.findById(id)
            .orElseThrow(() -> new NotFoundException("Endereço não encontrado para exclusão!"));
            addressRepositoryAdapter.deleteById(id);
        }
}
