package com.geosegbar.infra.security_level.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.SecurityLevelEntity;

@Repository
public interface SecurityLevelRepository extends JpaRepository<SecurityLevelEntity, Long> {
    List<SecurityLevelEntity> findAllByOrderByIdAsc();
    boolean existsByLevel(String level);
    boolean existsByLevelAndIdNot(String level, Long id);
}