package com.geosegbar.infra.dam.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DamRepository extends JpaRepository<DamModel, Long>{
    List<DamModel> findAllByOrderByIdAsc();
}
