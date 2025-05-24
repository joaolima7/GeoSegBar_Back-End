package com.geosegbar.infra.option.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.OptionEntity;

@Repository
public interface OptionRepository extends JpaRepository<OptionEntity, Long> {

    List<OptionEntity> findAllByOrderByOrderIndexAsc();

    Optional<OptionEntity> findByLabel(String label);
}
