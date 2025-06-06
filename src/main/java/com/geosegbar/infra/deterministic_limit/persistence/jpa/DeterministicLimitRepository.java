package com.geosegbar.infra.deterministic_limit.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DeterministicLimitEntity;

@Repository
public interface DeterministicLimitRepository extends JpaRepository<DeterministicLimitEntity, Long> {

    Optional<DeterministicLimitEntity> findByOutputId(Long outputId);

}
