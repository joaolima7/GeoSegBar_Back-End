package com.geosegbar.infra.client.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.core.client.entities.ClientEntity;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Long> {
    List<ClientEntity> findAllByOrderByIdAsc();

    boolean existsByName(String name);
    boolean existsByAcronym(String acronym);
    boolean existsByEmail(String email);
    boolean existsByNameAndIdNot(String name, Long id);
    boolean existsByAcronymAndIdNot(String acronym, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
}
