package com.geosegbar.infra.answer_photo.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnswerPhotoEntity;

@Repository
public interface AnswerPhotoRepository extends JpaRepository<AnswerPhotoEntity, Long> {

    @EntityGraph(attributePaths = {"answer"})
    List<AnswerPhotoEntity> findByAnswerId(Long answerId);

    @Override
    @EntityGraph(attributePaths = {"answer"})
    Optional<AnswerPhotoEntity> findById(Long id);
}
