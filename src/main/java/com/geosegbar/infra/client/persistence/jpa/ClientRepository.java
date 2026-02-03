package com.geosegbar.infra.client.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ClientEntity;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Long> {

    @EntityGraph(attributePaths = {"status"})
    List<ClientEntity> findAllByOrderByIdAsc();

    @Override
    @EntityGraph(attributePaths = {"status"})
    Optional<ClientEntity> findById(Long id);

    @Query("SELECT c FROM ClientEntity c WHERE (:statusId IS NULL OR c.status.id = :statusId)")
    @EntityGraph(attributePaths = {"status"})
    List<ClientEntity> findByStatus(@Param("statusId") Long statusId);

    boolean existsByName(String name);

    boolean existsByEmail(String email);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("SELECT COUNT(d) FROM DamEntity d WHERE d.client.id = :clientId")
    long countDamsByClientId(@Param("clientId") Long clientId);

    @Query("SELECT COUNT(u) FROM UserEntity u JOIN u.clients c WHERE c.id = :clientId")
    long countUsersByClientId(@Param("clientId") Long clientId);

    @Query("SELECT COUNT(dp) FROM DamPermissionEntity dp WHERE dp.client.id = :clientId")
    long countDamPermissionsByClientId(@Param("clientId") Long clientId);
}
