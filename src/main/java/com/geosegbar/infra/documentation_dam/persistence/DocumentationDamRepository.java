package com.geosegbar.infra.documentation_dam.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DocumentationDamEntity;

@Repository
public interface DocumentationDamRepository extends JpaRepository<DocumentationDamEntity, Long> {

    @EntityGraph(attributePaths = {"dam"})
    Optional<DocumentationDamEntity> findByDam(DamEntity dam);

    @EntityGraph(attributePaths = {"dam"})
    Optional<DocumentationDamEntity> findByDamId(Long damId);

    @Override
    @EntityGraph(attributePaths = {"dam"})
    List<DocumentationDamEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"dam"})
    Optional<DocumentationDamEntity> findById(Long id);

    boolean existsByDam(DamEntity dam);

    boolean existsByDamId(Long damId);
}
