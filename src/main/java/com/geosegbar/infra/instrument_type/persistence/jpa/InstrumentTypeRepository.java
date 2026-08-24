package com.geosegbar.infra.instrument_type.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentTypeEntity;

@Repository
public interface InstrumentTypeRepository extends JpaRepository<InstrumentTypeEntity, Long> {

    @EntityGraph(attributePaths = {"client"})
    List<InstrumentTypeEntity> findByClientIdOrderByNameAsc(Long clientId);

    @EntityGraph(attributePaths = {"client"})
    List<InstrumentTypeEntity> findByClientIdInOrderByNameAsc(List<Long> clientIds);

    /**
     * Tipos herdados do modelo antigo, sem cliente atrelado. Continuam válidos
     * para os instrumentos que já os usam, mas não podem ser editados nem
     * excluídos até serem migrados para um cliente.
     */
    @EntityGraph(attributePaths = {"client"})
    List<InstrumentTypeEntity> findByClientIsNullOrderByNameAsc();

    @EntityGraph(attributePaths = {"client"})
    Optional<InstrumentTypeEntity> findByClientIdAndNameIgnoreCase(Long clientId, String name);

    boolean existsByNameAndClientId(String name, Long clientId);

    boolean existsByNameAndClientIdAndIdNot(String name, Long clientId, Long id);

    @Query("SELECT COUNT(i) FROM InstrumentEntity i WHERE i.instrumentType.id = :typeId")
    long countInstrumentsByTypeId(@Param("typeId") Long typeId);

    @Query("SELECT i.instrumentType.id AS typeId, COUNT(i) AS total "
            + "FROM InstrumentEntity i WHERE i.instrumentType.id IN :typeIds "
            + "GROUP BY i.instrumentType.id")
    List<Object[]> countInstrumentsByTypeIds(@Param("typeIds") List<Long> typeIds);

    /**
     * Barragens distintas do cliente que já usam o tipo. Alimenta o aviso de
     * impacto na edição: renomear reflete em todas elas.
     */
    @Query("SELECT COUNT(DISTINCT i.dam.id) FROM InstrumentEntity i WHERE i.instrumentType.id = :typeId")
    long countDamsByTypeId(@Param("typeId") Long typeId);

    @Override
    @EntityGraph(attributePaths = {"client"})
    Optional<InstrumentTypeEntity> findById(Long id);

    @EntityGraph(attributePaths = {"client"})
    List<InstrumentTypeEntity> findAllByOrderByNameAsc();
}
