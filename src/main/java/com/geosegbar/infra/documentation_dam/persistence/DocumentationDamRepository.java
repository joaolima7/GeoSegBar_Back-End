package com.geosegbar.infra.documentation_dam.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DocumentationDamEntity;

@Repository
public interface DocumentationDamRepository extends JpaRepository<DocumentationDamEntity, Long> {
    
    Optional<DocumentationDamEntity> findByDam(DamEntity dam);
    
    Optional<DocumentationDamEntity> findByDamId(Long damId);
    
    boolean existsByDam(DamEntity dam);
    
    boolean existsByDamId(Long damId);
}