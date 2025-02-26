package com.geosegbar.infra.address.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<AddressModel, Long> {
    List<AddressModel> findAllByOrderByIdAsc();
}
