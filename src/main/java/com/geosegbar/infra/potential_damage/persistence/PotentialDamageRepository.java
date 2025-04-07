package com.geosegbar.infra.potential_damage.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.PotentialDamageEntity;

@Repository
public interface PotentialDamageRepository extends JpaRepository<PotentialDamageEntity, Long> {
    List<PotentialDamageEntity> findAllByOrderByIdAsc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
