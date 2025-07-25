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

    @EntityGraph(attributePaths = {"psbFolders"})
    Optional<DamEntity> findWithPsbFoldersById(Long id);

    @EntityGraph(attributePaths = {"reservoirs"})
    Optional<DamEntity> findWithReservoirsById(Long id);

    @EntityGraph(attributePaths = {"psbFolders"})
    List<DamEntity> findWithPsbFoldersByClientId(Long clientId);

    @EntityGraph(attributePaths = {"reservoirs"})
    List<DamEntity> findWithReservoirsByClientId(Long clientId);

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "WHERE (:clientId IS NULL OR d.client.id = :clientId) "
            + "AND (:statusId IS NULL OR d.status.id = :statusId)")
    List<DamEntity> findByClientAndStatus(
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId);

    @EntityGraph(attributePaths = {"psbFolders", "reservoirs"})
    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "WHERE (:clientId IS NULL OR d.client.id = :clientId) "
            + "AND (:statusId IS NULL OR d.status.id = :statusId)")
    List<DamEntity> findWithDetailsByClientAndStatus(
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId);
}
