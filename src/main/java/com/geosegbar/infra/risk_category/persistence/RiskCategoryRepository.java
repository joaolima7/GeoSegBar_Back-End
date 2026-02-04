package com.geosegbar.infra.risk_category.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.RiskCategoryEntity;

@Repository
public interface RiskCategoryRepository extends JpaRepository<RiskCategoryEntity, Long> {

    List<RiskCategoryEntity> findAllByOrderByIdAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
