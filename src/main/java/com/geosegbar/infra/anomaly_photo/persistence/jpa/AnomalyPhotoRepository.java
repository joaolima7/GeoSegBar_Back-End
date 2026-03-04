package com.geosegbar.infra.anomaly_photo.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnomalyPhotoEntity;
import com.geosegbar.infra.dashboard.projections.AnomalyPhotoPathProjection;

@Repository
public interface AnomalyPhotoRepository extends JpaRepository<AnomalyPhotoEntity, Long> {

    @Query("SELECT ap.anomaly.id as anomalyId, ap.imagePath as imagePath "
            + "FROM AnomalyPhotoEntity ap WHERE ap.anomaly.id IN :anomalyIds")
    List<AnomalyPhotoPathProjection> findPathsByAnomalyIds(@Param("anomalyIds") List<Long> anomalyIds);
}
