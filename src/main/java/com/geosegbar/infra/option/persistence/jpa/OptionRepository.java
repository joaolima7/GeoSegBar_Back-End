package com.geosegbar.infra.option.persistence.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.OptionEntity;

@Repository
public interface OptionRepository extends JpaRepository<OptionEntity, Long> {

    List<OptionEntity> findAllByOrderByOrderIndexAsc();

    Optional<OptionEntity> findByLabel(String label);

    /**
     * Opções de cada pergunta, na ordem de exibição (order_index, depois id).
     * Colunas: 0 question_id, 1 option_id, 2 option_label, 3 option_value.
     */
    @Query(value = """
            SELECT qo.question_id AS question_id,
                   o.id AS option_id,
                   o.label AS option_label,
                   o.value AS option_value
            FROM question_option qo
            JOIN options o ON o.id = qo.option_id
            WHERE qo.question_id IN :questionIds
            ORDER BY qo.question_id, o.order_index, o.id
            """, nativeQuery = true)
    List<Object[]> findOptionRowsByQuestionIds(@Param("questionIds") Collection<Long> questionIds);
}
