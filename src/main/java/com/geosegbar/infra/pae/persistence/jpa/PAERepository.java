package com.geosegbar.infra.pae.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.PAEEntity;

@Repository
public interface PAERepository extends JpaRepository<PAEEntity, Long> {

    @EntityGraph(attributePaths = {"protectionElements", "contacts"})
    Optional<PAEEntity> findByDamId(Long damId);

    boolean existsByDamId(Long damId);

    @Override
    @EntityGraph(attributePaths = {"protectionElements", "contacts"})
    Optional<PAEEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"protectionElements", "contacts"})
    List<PAEEntity> findAll();
}
