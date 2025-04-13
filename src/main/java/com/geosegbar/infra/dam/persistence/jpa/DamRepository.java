package com.geosegbar.infra.dam.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;

@Repository
public interface DamRepository extends JpaRepository<DamEntity, Long>{
    List<DamEntity> findAllByOrderByIdAsc();
    List<DamEntity> findByClient(ClientEntity client);
    List<DamEntity> findByClientId(Long clientId);  

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
