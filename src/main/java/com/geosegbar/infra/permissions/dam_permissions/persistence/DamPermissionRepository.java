package com.geosegbar.infra.permissions.dam_permissions.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.UserEntity;

@Repository
public interface DamPermissionRepository extends JpaRepository<DamPermissionEntity, Long> {

    @EntityGraph(attributePaths = {"user", "dam", "client"})
    List<DamPermissionEntity> findByUserAndHasAccessTrue(UserEntity user);

    @EntityGraph(attributePaths = {"user", "dam", "client"})
    List<DamPermissionEntity> findByUserAndClientAndHasAccessTrue(UserEntity user, ClientEntity client);

    @EntityGraph(attributePaths = {"user", "dam", "client"})
    Optional<DamPermissionEntity> findByUserAndDamAndClient(UserEntity user, DamEntity dam, ClientEntity client);

    boolean existsByUserAndDamAndClient(UserEntity user, DamEntity dam, ClientEntity client);

    @EntityGraph(attributePaths = {"user", "client"})
    List<DamPermissionEntity> findByDam(DamEntity dam);

    @EntityGraph(attributePaths = {"user", "dam"})
    List<DamPermissionEntity> findByClient(ClientEntity client);

    @EntityGraph(attributePaths = {"dam", "client"})
    List<DamPermissionEntity> findByUser(UserEntity user);

    @EntityGraph(attributePaths = {"dam"})
    List<DamPermissionEntity> findByUserAndClient(UserEntity user, ClientEntity client);

    @Modifying
    @Query("DELETE FROM DamPermissionEntity dp WHERE dp.user = :user AND dp.client = :client")
    void deleteByUserAndClient(@Param("user") UserEntity user, @Param("client") ClientEntity client);

    @Modifying
    @Query("DELETE FROM DamPermissionEntity dp "
            + "WHERE dp.user = :user "
            + "AND dp.dam = :dam "
            + "AND dp.client = :client")
    void deleteByUserAndDamAndClient(
            @Param("user") UserEntity user,
            @Param("dam") DamEntity dam,
            @Param("client") ClientEntity client
    );

    @Modifying
    @Query("DELETE FROM DamPermissionEntity dp WHERE dp.dam.id = :damId")
    void deleteByDamId(@Param("damId") Long damId);

    @Modifying
    @Query("DELETE FROM DamPermissionEntity dp WHERE dp.dam.id = :damId AND dp.client.id = :clientId")
    void deleteByDamIdAndClientId(@Param("damId") Long damId, @Param("clientId") Long clientId);

    @Modifying
    @Query("DELETE FROM DamPermissionEntity dp WHERE dp.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM DamPermissionEntity dp WHERE dp.user.id = :userId AND dp.client.id = :clientId")
    void deleteByUserIdAndClientId(@Param("userId") Long userId, @Param("clientId") Long clientId);
}
