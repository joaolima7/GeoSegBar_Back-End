package com.geosegbar.adapters.address;

import java.util.List;
import java.util.Optional;

import com.geosegbar.core.address.entities.AddressEntity;

public interface AddressRepositoryAdapter {
    void deleteById(Long id);
    AddressEntity save(AddressEntity addressEntity);
    AddressEntity update(AddressEntity addressEntity);
    Optional<AddressEntity> findById(Long id);
    List<AddressEntity> findAll();
}
