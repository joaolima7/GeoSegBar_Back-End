package com.geosegbar.infra.user.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.RoleEnum;
import com.geosegbar.entities.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.status WHERE u.email = :email")
    Optional<UserEntity> findByEmailForAuthentication(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.role "
            + "LEFT JOIN FETCH u.status "
            + "LEFT JOIN FETCH u.sex "
            + "WHERE u.email = :email")
    Optional<UserEntity> findByEmailWithBasicDetails(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.clients c "
            + "LEFT JOIN FETCH u.sex "
            + "LEFT JOIN FETCH u.status "
            + "LEFT JOIN FETCH u.role "
            + "LEFT JOIN FETCH u.createdBy cb "
            + "ORDER BY u.id ASC")
    List<UserEntity> findAllWithBasicDetails();

    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.clients c WHERE u.id IN :ids")
    List<UserEntity> findByIdInWithClients(@Param("ids") Set<Long> ids);

    @Query("SELECT DISTINCT u FROM UserEntity u JOIN u.clients c WHERE c.id = :clientId AND u.role.name = :roleName")
    List<UserEntity> findByClientIdAndRole(@Param("clientId") Long clientId, @Param("roleName") RoleEnum roleName);

    @Query("SELECT u FROM UserEntity u JOIN u.clients c WHERE c.id = :clientId")
    List<UserEntity> findByClientId(@Param("clientId") Long clientId);

    @EntityGraph(attributePaths = {"clients", "sex", "status", "role", "createdBy"})
    @Query("SELECT DISTINCT u FROM UserEntity u "
            + "WHERE u.name != 'SISTEMA' "
            + "AND u.email != 'noreply@geometrisa-prod.com.br' "
            + "AND (:statusId IS NULL OR u.status.id = :statusId) "
            + "AND ("
            + "    (:clientId IS NULL) "
            + "    OR (EXISTS (SELECT 1 FROM u.clients c WHERE c.id = :clientId)) "
            + "    OR (u.role.name = 'COLLABORATOR' AND u.clients IS EMPTY)"
            + ") "
            + "ORDER BY u.id ASC")
    List<UserEntity> findByClientIncludingUnassignedCollaborators(
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId);

    @EntityGraph(attributePaths = {
        "clients", "sex", "status", "role", "createdBy",
        "attributionsPermission",
        "documentationPermission",
        "instrumentationPermission",
        "routineInspectionPermission"
    })
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdWithAllDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"clients", "createdBy"})
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdWithClients(@Param("id") Long id);

    @EntityGraph(attributePaths = {
        "clients",
        "damPermissions",
        "damPermissions.dam",
        "damPermissions.client",
        "routineInspectionPermission"
    })
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdWithPermissions(@Param("id") Long id);

    @EntityGraph(attributePaths = {"sex", "status", "role"})
    Optional<UserEntity> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = {"sex", "status", "role"})
    List<UserEntity> findAll();

    @EntityGraph(attributePaths = {"clients", "sex", "status", "role", "createdBy"})
    @Query("SELECT DISTINCT u FROM UserEntity u "
            + "WHERE u.name != 'SISTEMA' "
            + "AND u.email != 'noreply@geometrisa-prod.com.br' "
            + "AND (:roleId IS NULL OR u.role.id = :roleId) "
            + "AND (:statusId IS NULL OR u.status.id = :statusId) "
            + "AND ("
            + "    (:clientId IS NULL) "
            + "    OR (EXISTS (SELECT 1 FROM u.clients c WHERE c.id = :clientId)) "
            + "    OR (:withoutClient = true AND u.clients IS EMPTY)"
            + ") "
            + "ORDER BY u.id ASC")
    List<UserEntity> findByRoleAndClientWithDetails(
            @Param("roleId") Long roleId,
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId,
            @Param("withoutClient") Boolean withoutClient);

    @EntityGraph(attributePaths = {"clients", "sex", "status", "role", "createdBy"})
    List<UserEntity> findByCreatedById(Long createdById);

    @EntityGraph(attributePaths = {
        "clients", "sex", "status", "role", "createdBy",
        "attributionsPermission",
        "documentationPermission",
        "instrumentationPermission",
        "routineInspectionPermission"
    })
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email")
    Optional<UserEntity> findByEmailWithAllPermissions(@Param("email") String email);

    @EntityGraph(attributePaths = {"clients", "sex", "status", "role", "createdBy"})
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email")
    Optional<UserEntity> findByEmailWithClientsAndDetails(@Param("email") String email);

    boolean existsByName(String name);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Query("SELECT u.id FROM UserEntity u JOIN u.clients c WHERE c.id = :clientId")
    List<Long> findUserIdsByClientId(@Param("clientId") Long clientId);

    @Modifying
    @Query("UPDATE UserEntity u SET u.status.id = :statusId WHERE u.id IN :userIds")
    int bulkUpdateStatusByIds(@Param("userIds") List<Long> userIds, @Param("statusId") Long statusId);

    @Query("SELECT u.id FROM UserEntity u WHERE u.email = 'noreply@geometrisa-prod.com.br'")
    Optional<Long> findSystemUserId();
}
