package com.geosegbar.infra.user.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.RoleEnum;
import com.geosegbar.entities.UserEntity;

import jakarta.persistence.QueryHint;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query("SELECT DISTINCT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.clients c "
            + "WHERE u.id IN :ids")
    List<UserEntity> findByIdInWithClients(@Param("ids") Set<Long> ids);

    @Query("SELECT DISTINCT u FROM UserEntity u "
            + "JOIN u.clients c "
            + "WHERE c.id = :clientId "
            + "AND u.role.name = :roleName")
    List<UserEntity> findByClientIdAndRole(
            @Param("clientId") Long clientId,
            @Param("roleName") RoleEnum roleName
    );

    @Query("SELECT u FROM UserEntity u "
            + "JOIN u.clients c "
            + "WHERE c.id = :clientId")
    List<UserEntity> findByClientId(@Param("clientId") Long clientId);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT DISTINCT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.clients c "
            + "LEFT JOIN FETCH u.sex "
            + "LEFT JOIN FETCH u.status "
            + "LEFT JOIN FETCH u.role r "
            + "LEFT JOIN FETCH u.createdBy cb "
            + "WHERE u.name != 'SISTEMA' "
            + "AND u.email != 'noreply@geometrisa-prod.com.br' "
            + "AND (:statusId IS NULL OR u.status.id = :statusId) "
            + "AND ("
            + "    (c.id = :clientId) "
            + "    OR (r.name = 'COLLABORATOR' AND (SELECT COUNT(uc) FROM u.clients uc) = 0)"
            + ") "
            + "ORDER BY u.id ASC")
    List<UserEntity> findByClientIncludingUnassignedCollaborators(
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.clients c "
            + "LEFT JOIN FETCH u.sex "
            + "LEFT JOIN FETCH u.status "
            + "LEFT JOIN FETCH u.role "
            + "LEFT JOIN FETCH u.createdBy cb "
            + "LEFT JOIN FETCH u.attributionsPermission "
            + "LEFT JOIN FETCH u.documentationPermission "
            + "LEFT JOIN FETCH u.instrumentationPermission "
            + "LEFT JOIN FETCH u.routineInspectionPermission "
            + "WHERE u.id = :id")
    Optional<UserEntity> findByIdWithAllDetails(@Param("id") Long id);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.clients c "
            + "LEFT JOIN FETCH u.createdBy cb "
            + "WHERE u.id = :id")
    Optional<UserEntity> findByIdWithClients(@Param("id") Long id);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.sex "
            + "LEFT JOIN FETCH u.status "
            + "LEFT JOIN FETCH u.role "
            + "WHERE u.email = :email")
    Optional<UserEntity> findByEmailWithBasicDetails(@Param("email") String email);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.clients c "
            + "LEFT JOIN FETCH u.sex "
            + "LEFT JOIN FETCH u.status "
            + "LEFT JOIN FETCH u.role "
            + "LEFT JOIN FETCH u.createdBy cb "
            + "ORDER BY u.id ASC")
    List<UserEntity> findAllWithBasicDetails();

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT DISTINCT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.clients c "
            + "LEFT JOIN FETCH u.sex "
            + "LEFT JOIN FETCH u.status "
            + "LEFT JOIN FETCH u.role "
            + "LEFT JOIN FETCH u.createdBy cb "
            + "WHERE u.name != 'SISTEMA' "
            + "AND u.email != 'noreply@geometrisa-prod.com.br' "
            + "AND (:roleId IS NULL OR u.role.id = :roleId) "
            + "AND (:statusId IS NULL OR u.status.id = :statusId) "
            + "AND ("
            + "    (:clientId IS NULL) "
            + "    OR (c.id = :clientId) "
            + "    OR (:withoutClient = true AND (SELECT COUNT(uc) FROM u.clients uc) = 0)"
            + ") "
            + "ORDER BY u.id ASC")
    List<UserEntity> findByRoleAndClientWithDetails(
            @Param("roleId") Long roleId,
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId,
            @Param("withoutClient") Boolean withoutClient);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.clients c "
            + "LEFT JOIN FETCH u.sex "
            + "LEFT JOIN FETCH u.status "
            + "LEFT JOIN FETCH u.role "
            + "LEFT JOIN FETCH u.createdBy cb "
            + "WHERE u.createdBy.id = :createdById "
            + "ORDER BY u.id ASC")
    List<UserEntity> findByCreatedByIdWithDetails(@Param("createdById") Long createdById);

    List<UserEntity> findAllByOrderByIdAsc();

    Optional<UserEntity> findByEmail(String email);

    @Query("SELECT DISTINCT u FROM UserEntity u "
            + "LEFT JOIN u.clients c "
            + "WHERE (:roleId IS NULL OR u.role.id = :roleId) "
            + "AND (:clientId IS NULL OR c.id = :clientId) "
            + "AND (:statusId IS NULL OR u.status.id = :statusId)")
    List<UserEntity> findByRoleAndClient(
            @Param("roleId") Long roleId,
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId);

    List<UserEntity> findByCreatedById(Long createdById);

    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT u FROM UserEntity u "
            + "LEFT JOIN FETCH u.clients c "
            + "LEFT JOIN FETCH u.sex "
            + "LEFT JOIN FETCH u.role "
            + "WHERE u.email = :email")
    Optional<UserEntity> findByEmailWithClientsAndDetails(@Param("email") String email);

    boolean existsByName(String name);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
