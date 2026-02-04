package com.geosegbar.infra.dam.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.StatusEntity;

@Repository
public interface DamRepository extends JpaRepository<DamEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"client", "status"})
    Optional<DamEntity> findById(Long id);

    @EntityGraph(attributePaths = {"client", "status"})
    List<DamEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"client", "status"})
    List<DamEntity> findByClientId(Long clientId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByNameAndClientId(String name, Long clientId);

    boolean existsByNameAndClientIdAndIdNot(String name, Long clientId, Long id);

    @Modifying
    @Query("UPDATE DamEntity d SET d.status = :status WHERE d.client.id = :clientId")
    int updateStatusByClientId(@Param("clientId") Long clientId, @Param("status") StatusEntity status);

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "LEFT JOIN FETCH d.client c "
            + "LEFT JOIN FETCH c.status "
            + "LEFT JOIN FETCH d.status "
            + "LEFT JOIN FETCH d.regulatoryDam rd "
            + "LEFT JOIN FETCH rd.securityLevel "
            + "LEFT JOIN FETCH rd.riskCategory "
            + "LEFT JOIN FETCH rd.potentialDamage "
            + "LEFT JOIN FETCH rd.classificationDam "
            + "LEFT JOIN FETCH d.documentationDam "
            + "ORDER BY d.id ASC")
    List<DamEntity> findAllComplete();

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "LEFT JOIN FETCH d.client c "
            + "LEFT JOIN FETCH c.status "
            + "LEFT JOIN FETCH d.status "
            + "LEFT JOIN FETCH d.regulatoryDam rd "
            + "LEFT JOIN FETCH rd.securityLevel "
            + "LEFT JOIN FETCH rd.riskCategory "
            + "LEFT JOIN FETCH rd.potentialDamage "
            + "LEFT JOIN FETCH rd.classificationDam "
            + "LEFT JOIN FETCH d.documentationDam "
            + "WHERE (:clientId IS NULL OR d.client.id = :clientId) "
            + "AND (:statusId IS NULL OR d.status.id = :statusId) "
            + "ORDER BY d.id ASC")
    List<DamEntity> findByClientAndStatusComplete(
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId);

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "LEFT JOIN FETCH d.client c "
            + "LEFT JOIN FETCH c.status "
            + "LEFT JOIN FETCH d.status "
            + "LEFT JOIN FETCH d.regulatoryDam rd "
            + "LEFT JOIN FETCH rd.securityLevel "
            + "LEFT JOIN FETCH rd.riskCategory "
            + "LEFT JOIN FETCH rd.potentialDamage "
            + "LEFT JOIN FETCH rd.classificationDam "
            + "LEFT JOIN FETCH d.documentationDam "
            + "WHERE d.id = :id")
    Optional<DamEntity> findByIdComplete(@Param("id") Long id);
}
