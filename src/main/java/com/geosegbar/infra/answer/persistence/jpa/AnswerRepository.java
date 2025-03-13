package com.geosegbar.infra.answer.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnswerEntity;

import jakarta.persistence.EntityManager;

@Repository
public interface AnswerRepository extends JpaRepository<AnswerEntity, Long> {
    
}

interface AnswerRepositoryCustom {
    EntityManager getEntityManager();
}
