package com.geosegbar.infra.regulatory_dam.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.RegulatoryDamEntity;

@Repository
public interface RegulatoryDamRepository extends JpaRepository<RegulatoryDamEntity, Long> {
    
    Optional<RegulatoryDamEntity> findByDam(DamEntity dam);
    
    Optional<RegulatoryDamEntity> findByDamId(Long damId);
    
    boolean existsByDam(DamEntity dam);
    
    boolean existsByDamId(Long damId);
}