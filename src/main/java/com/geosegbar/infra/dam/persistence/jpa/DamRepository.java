package com.geosegbar.infra.dam.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.core.dam.entities.DamEntity;

@Repository
public interface DamRepository extends JpaRepository<DamEntity, Long>{
    List<DamEntity> findAllByOrderByIdAsc();

    boolean existsByName(String name);
    boolean existsByAcronym(String acronym);
    boolean existsByNameAndIdNot(String name, Long id);
    boolean existsByAcronymAndIdNot(String acronym, Long id);
}
