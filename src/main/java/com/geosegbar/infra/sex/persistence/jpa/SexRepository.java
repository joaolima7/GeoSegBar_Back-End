package com.geosegbar.infra.sex.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SexRepository extends JpaRepository<SexModel, Long> {
    List<SexModel> findAllByOrderByIdAsc();
    
}
