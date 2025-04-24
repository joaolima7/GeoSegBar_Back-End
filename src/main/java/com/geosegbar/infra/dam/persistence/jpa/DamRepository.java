package com.geosegbar.infra.dam.persistence.jpa;

import java.util.List;
import java.util.Optional;

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

    @Query("SELECT d FROM DamEntity d LEFT JOIN FETCH d.reservoirs LEFT JOIN FETCH d.psbFolders WHERE d.id = :id")
    Optional<DamEntity> findByIdWithReservoirsAndFolders(@Param("id") Long id);

    @Query("SELECT DISTINCT d FROM DamEntity d LEFT JOIN FETCH d.reservoirs LEFT JOIN FETCH d.psbFolders ORDER BY d.id ASC")
    List<DamEntity> findAllWithReservoirsAndFolders();

    @Query("SELECT DISTINCT d FROM DamEntity d LEFT JOIN FETCH d.reservoirs LEFT JOIN FETCH d.psbFolders WHERE d.client.id = :clientId")
    List<DamEntity> findByClientIdWithReservoirsAndFolders(@Param("clientId") Long clientId);
}
