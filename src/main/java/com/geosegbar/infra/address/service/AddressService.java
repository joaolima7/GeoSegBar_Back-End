package com.geosegbar.infra.address.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.core.address.entities.AddressEntity;
import com.geosegbar.core.address.usecases.CreateAddresUseCase;
import com.geosegbar.core.address.usecases.DeleteAddressUseCase;
import com.geosegbar.core.address.usecases.FindAllAddressUseCase;
import com.geosegbar.core.address.usecases.FindByIdAddressUseCase;
import com.geosegbar.core.address.usecases.UpdateAddressUseCase;

@Service
public class AddressService {
    private final FindAllAddressUseCase findAllAddressUseCase;
    private final FindByIdAddressUseCase findByIdAddressUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final CreateAddresUseCase createAddresUseCase;

    public AddressService(FindAllAddressUseCase findAllAddressUseCase, 
                          FindByIdAddressUseCase findByIdAddressUseCase,
                          DeleteAddressUseCase deleteAddressUseCase,
                          UpdateAddressUseCase updateAddressUseCase,
                          CreateAddresUseCase createAddresUseCase) {
        this.findAllAddressUseCase = findAllAddressUseCase;
        this.findByIdAddressUseCase = findByIdAddressUseCase;
        this.deleteAddressUseCase = deleteAddressUseCase;
        this.updateAddressUseCase = updateAddressUseCase;
        this.createAddresUseCase = createAddresUseCase;
    }

    public AddressEntity create(AddressEntity address) {
        return createAddresUseCase.create(address);
    }

    public AddressEntity update(AddressEntity address) {
        return updateAddressUseCase.update(address);
    }

    public void delete(Long id) {
         deleteAddressUseCase.delete(id);
    }

    public AddressEntity findById(Long id) {
         return findByIdAddressUseCase.findById(id);
    }

    public List<AddressEntity> findAll() {
         return findAllAddressUseCase.findAll();
    }

}
