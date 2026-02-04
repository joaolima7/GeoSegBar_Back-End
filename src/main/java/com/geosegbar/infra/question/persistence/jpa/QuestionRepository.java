package com.geosegbar.infra.question.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.QuestionEntity;

@Repository
public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {

    @EntityGraph(attributePaths = {"client", "options"})
    List<QuestionEntity> findByClientId(Long clientId);

    @EntityGraph(attributePaths = {"client", "options"})
    List<QuestionEntity> findByClientIdOrderByQuestionTextAsc(Long clientId);

    @EntityGraph(attributePaths = {"client", "options"})
    @Query("SELECT q FROM QuestionEntity q WHERE q.id = :id")
    QuestionEntity findByIdWithDetails(@Param("id") Long id);

    boolean existsByQuestionTextAndClientId(String questionText, Long clientId);

    boolean existsByQuestionTextAndClientIdAndIdNot(String questionText, Long clientId, Long id);

    @Override
    @EntityGraph(attributePaths = {"client", "options"})
    List<QuestionEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"client", "options"})
    Optional<QuestionEntity> findById(Long id);
}
