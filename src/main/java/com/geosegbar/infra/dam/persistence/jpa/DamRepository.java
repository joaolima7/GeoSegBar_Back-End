package com.geosegbar.infra.dam.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;

@Repository
public interface DamRepository extends JpaRepository<DamEntity, Long> {

    List<DamEntity> findAllByOrderByIdAsc();

    List<DamEntity> findByClient(ClientEntity client);

    List<DamEntity> findByClientId(Long clientId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByNameAndClientId(String name, Long clientId);

    boolean existsByNameAndClientIdAndIdNot(String name, Long clientId, Long id);

    @EntityGraph(attributePaths = {"psbFolders"})
    Optional<DamEntity> findWithPsbFoldersById(Long id);

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "LEFT JOIN FETCH d.reservoirs r "
            + "LEFT JOIN FETCH r.level "
            + "WHERE d.id = :id")
    Optional<DamEntity> findWithReservoirsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"psbFolders"})
    List<DamEntity> findWithPsbFoldersByClientId(Long clientId);

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "LEFT JOIN FETCH d.reservoirs r "
            + "LEFT JOIN FETCH r.level "
            + "WHERE d.client.id = :clientId")
    List<DamEntity> findWithReservoirsByClientId(@Param("clientId") Long clientId);

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "WHERE (:clientId IS NULL OR d.client.id = :clientId) "
            + "AND (:statusId IS NULL OR d.status.id = :statusId)")
    List<DamEntity> findByClientAndStatus(
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId);

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "LEFT JOIN FETCH d.psbFolders "
            + "LEFT JOIN FETCH d.reservoirs r "
            + "LEFT JOIN FETCH r.level "
            + "WHERE (:clientId IS NULL OR d.client.id = :clientId) "
            + "AND (:statusId IS NULL OR d.status.id = :statusId)")
    List<DamEntity> findWithDetailsByClientAndStatus(
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId);
}
