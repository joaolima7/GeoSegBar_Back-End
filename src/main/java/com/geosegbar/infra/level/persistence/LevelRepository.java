package com.geosegbar.infra.level.persistence;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.LevelEntity;

@Repository
public interface LevelRepository extends JpaRepository<LevelEntity, Long> {
    List<LevelEntity> findAllByOrderByIdAsc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    Optional<LevelEntity> findByName(String name);
}
