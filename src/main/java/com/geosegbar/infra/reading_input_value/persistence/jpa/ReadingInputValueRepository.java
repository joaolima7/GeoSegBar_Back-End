package com.geosegbar.infra.reading_input_value.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ReadingInputValueEntity;

@Repository
public interface ReadingInputValueRepository extends JpaRepository<ReadingInputValueEntity, Long> {

    List<ReadingInputValueEntity> findByReadingId(Long readingId);

    void deleteByReadingId(Long readingId);
}
