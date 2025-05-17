package com.geosegbar.infra.anomaly_photo.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnomalyPhotoEntity;

@Repository
public interface AnomalyPhotoRepository extends JpaRepository<AnomalyPhotoEntity, Long> {
}
