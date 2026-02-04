package com.geosegbar.infra.regulatory_dam.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.RegulatoryDamEntity;

@Repository
public interface RegulatoryDamRepository extends JpaRepository<RegulatoryDamEntity, Long> {

    @EntityGraph(attributePaths = {
        "dam",
        "securityLevel",
        "riskCategory",
        "potentialDamage",
        "classificationDam"
    })
    Optional<RegulatoryDamEntity> findByDam(DamEntity dam);

    @EntityGraph(attributePaths = {
        "dam",
        "securityLevel",
        "riskCategory",
        "potentialDamage",
        "classificationDam"
    })
    Optional<RegulatoryDamEntity> findByDamId(Long damId);

    boolean existsByDam(DamEntity dam);

    boolean existsByDamId(Long damId);

    @Override
    @EntityGraph(attributePaths = {
        "dam",
        "securityLevel",
        "riskCategory",
        "potentialDamage",
        "classificationDam"
    })
    Optional<RegulatoryDamEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {
        "dam",
        "securityLevel",
        "riskCategory",
        "potentialDamage",
        "classificationDam"
    })
    List<RegulatoryDamEntity> findAll();
}
