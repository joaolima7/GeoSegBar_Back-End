package com.geosegbar.infra.reading_input_value.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ReadingInputValueEntity;

@Repository
public interface ReadingInputValueRepository extends JpaRepository<ReadingInputValueEntity, Long> {

    @Query("SELECT riv FROM ReadingInputValueEntity riv JOIN riv.readings r WHERE r.id = :readingId")
    List<ReadingInputValueEntity> findByReadingId(@Param("readingId") Long readingId);

    @Modifying
    @Query("DELETE FROM ReadingInputValueEntity riv WHERE riv.id IN "
            + "(SELECT riv.id FROM ReadingInputValueEntity riv JOIN riv.readings r WHERE r.id = :readingId)")
    void deleteByReadingId(@Param("readingId") Long readingId);
}
