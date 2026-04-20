package com.geosegbar.infra.map_kml.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.MapKmlFileEntity;

@Repository
public interface MapKmlFileRepository extends JpaRepository<MapKmlFileEntity, Long> {

    List<MapKmlFileEntity> findByFolderId(Long folderId);
}
