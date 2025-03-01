package com.geosegbar.infra.sex.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.SexEntity;

@Repository
public interface SexRepository extends JpaRepository<SexEntity, Long> {
    List<SexEntity> findAllByOrderByIdAsc();

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
