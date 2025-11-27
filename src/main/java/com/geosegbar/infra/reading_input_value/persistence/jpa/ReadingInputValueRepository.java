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

    /**
     * ⭐ OTIMIZADO: Busca direta por FK (sem JOIN na tabela de junção) Antes:
     * SELECT riv FROM ReadingInputValueEntity riv JOIN riv.readings r WHERE
     * r.id = :readingId Agora: SELECT riv FROM ReadingInputValueEntity riv
     * WHERE riv.reading.id = :readingId
     */
    List<ReadingInputValueEntity> findByReadingId(Long readingId);

    /**
     * ⭐ NOVO: Busca em batch para eliminar N+1 Permite carregar inputValues de
     * múltiplas readings de uma vez
     */
    @Query("SELECT riv FROM ReadingInputValueEntity riv WHERE riv.reading.id IN :readingIds")
    List<ReadingInputValueEntity> findByReadingIdIn(@Param("readingIds") List<Long> readingIds);

    /**
     * ⭐ OTIMIZADO: Delete direto por FK
     */
    @Modifying
    @Query("DELETE FROM ReadingInputValueEntity riv WHERE riv.reading.id = :readingId")
    void deleteByReadingId(@Param("readingId") Long readingId);

    /**
     * ⭐ NOVO: Delete em batch
     */
    @Modifying
    @Query("DELETE FROM ReadingInputValueEntity riv WHERE riv.reading.id IN :readingIds")
    void deleteByReadingIdIn(@Param("readingIds") List<Long> readingIds);

    /**
     * ⭐ NOVO: Verifica se existem inputValues para uma reading
     */
    boolean existsByReadingId(Long readingId);

    /**
     * ⭐ NOVO: Conta inputValues por reading
     */
    long countByReadingId(Long readingId);

    /**
     * ⭐ NOVO: Busca inputValue específico por reading e acronym
     */
    @Query("SELECT riv FROM ReadingInputValueEntity riv WHERE riv.reading.id = :readingId AND riv.inputAcronym = :acronym")
    ReadingInputValueEntity findByReadingIdAndInputAcronym(
            @Param("readingId") Long readingId,
            @Param("acronym") String acronym);
}
