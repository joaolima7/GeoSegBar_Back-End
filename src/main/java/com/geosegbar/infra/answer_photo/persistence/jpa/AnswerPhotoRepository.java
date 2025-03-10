package com.geosegbar.infra.answer_photo.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnswerPhotoEntity;

@Repository
public interface AnswerPhotoRepository extends JpaRepository<AnswerPhotoEntity, Long> {
    List<AnswerPhotoEntity> findByAnswerId(Long answerId);
}
