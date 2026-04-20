package com.geosegbar.infra.map_kml.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.MapKmlFolderEntity;

@Repository
public interface MapKmlFolderRepository extends JpaRepository<MapKmlFolderEntity, Long> {

    @EntityGraph(attributePaths = {"files"})
    List<MapKmlFolderEntity> findByDamId(Long damId);

    boolean existsByNameAndDamId(String name, Long damId);

    boolean existsByNameAndDamIdAndIdNot(String name, Long damId, Long id);
}
