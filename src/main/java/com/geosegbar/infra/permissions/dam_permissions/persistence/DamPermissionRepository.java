package com.geosegbar.infra.permissions.dam_permissions.persistence;

import java.util.List;
import java.util.Optional;

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

    List<DamPermissionEntity> findByUserAndHasAccessTrue(UserEntity user);

    List<DamPermissionEntity> findByUserAndClientAndHasAccessTrue(UserEntity user, ClientEntity client);

    Optional<DamPermissionEntity> findByUserAndDamAndClient(UserEntity user, DamEntity dam, ClientEntity client);

    boolean existsByUserAndDamAndClient(UserEntity user, DamEntity dam, ClientEntity client);

    List<DamPermissionEntity> findByDam(DamEntity dam);

    List<DamPermissionEntity> findByClient(ClientEntity client);

    List<DamPermissionEntity> findByUser(UserEntity user);

    List<DamPermissionEntity> findByUserAndClient(UserEntity user, ClientEntity client);

    void deleteByUserAndClient(UserEntity user, ClientEntity client);

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
}
