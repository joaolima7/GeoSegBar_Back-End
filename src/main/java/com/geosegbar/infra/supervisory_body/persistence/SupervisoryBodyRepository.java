package com.geosegbar.infra.supervisory_body.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.SupervisoryBodyEntity;

@Repository
public interface SupervisoryBodyRepository extends JpaRepository<SupervisoryBodyEntity, Long> {
    List<SupervisoryBodyEntity> findAllByOrderByIdAsc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
