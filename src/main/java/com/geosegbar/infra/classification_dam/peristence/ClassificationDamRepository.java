package com.geosegbar.infra.classification_dam.peristence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ClassificationDamEntity;

@Repository
public interface ClassificationDamRepository extends JpaRepository<ClassificationDamEntity, Long> {
    List<ClassificationDamEntity> findAllByOrderByIdAsc();
    boolean existsByClassification(String classification);
    boolean existsByClassificationAndIdNot(String classification, Long id);
}
