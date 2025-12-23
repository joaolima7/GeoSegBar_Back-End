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

    List<ReadingInputValueEntity> findByReadingId(Long readingId);

    @Query("SELECT riv FROM ReadingInputValueEntity riv WHERE riv.reading.id IN :readingIds")
    List<ReadingInputValueEntity> findByReadingIdIn(@Param("readingIds") List<Long> readingIds);

    @Modifying
    @Query("DELETE FROM ReadingInputValueEntity riv WHERE riv.reading.id = :readingId")
    void deleteByReadingId(@Param("readingId") Long readingId);

    @Modifying
    @Query("DELETE FROM ReadingInputValueEntity riv WHERE riv.reading.id IN :readingIds")
    void deleteByReadingIdIn(@Param("readingIds") List<Long> readingIds);

    boolean existsByReadingId(Long readingId);

    long countByReadingId(Long readingId);

    @Query("SELECT riv FROM ReadingInputValueEntity riv WHERE riv.reading.id = :readingId AND riv.inputAcronym = :acronym")
    ReadingInputValueEntity findByReadingIdAndInputAcronym(
            @Param("readingId") Long readingId,
            @Param("acronym") String acronym);
}
