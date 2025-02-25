package com.geosegbar.infra.address.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geosegbar.adapters.address.AddressRepositoryAdapter;
import com.geosegbar.core.address.entities.AddressEntity;
import com.geosegbar.infra.address.dto.AddressHandler;

@Component
public class AddressJpaRespositoryImp implements AddressRepositoryAdapter{
    private final AddressRepository addressRepository;

    @Autowired
    private AddressHandler addressHandler;

    public AddressJpaRespositoryImp(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public void deleteById(Long id) {
        addressRepository.deleteById(id);
    }

    @Override
    public AddressEntity save(AddressEntity addressEntity) {
        AddressModel addressModel = addressRepository.save(addressHandler.fromEntity(addressEntity).toModel());
        return addressHandler.fromModel(addressModel).toEntity();
    }

    @Override
    public AddressEntity update(AddressEntity addressEntity) {
        AddressModel addressModel = addressRepository.save(addressHandler.fromEntity(addressEntity).toModel());
        return addressHandler.fromModel(addressModel).toEntity();
    }

    @Override
    public Optional<AddressEntity> findById(Long id) {
        Optional<AddressModel> addressModel = addressRepository.findById(id);
        return addressModel.map(model -> addressHandler.fromModel(model).toEntity());
    }

    @Override
    public List<AddressEntity> findAll() {
        List<AddressModel> addressModels = addressRepository.findAll();
        return addressModels.stream().map(addressModel -> addressHandler.fromModel(addressModel).toEntity()).toList();
    }


}
