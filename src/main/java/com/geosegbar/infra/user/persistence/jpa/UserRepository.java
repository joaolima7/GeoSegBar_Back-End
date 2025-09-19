package com.geosegbar.infra.user.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.UserEntity;

import jakarta.persistence.QueryHint;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

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
            + "WHERE (:roleId IS NULL OR u.role.id = :roleId) "
            + "AND (:clientId IS NULL OR c.id = :clientId) "
            + "AND (:statusId IS NULL OR u.status.id = :statusId) "
            + "ORDER BY u.id ASC")
    List<UserEntity> findByRoleAndClientWithDetails(
            @Param("roleId") Long roleId,
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
            + "WHERE u.createdBy.id = :createdById "
            + "ORDER BY u.id ASC")
    List<UserEntity> findByCreatedByIdWithDetails(@Param("createdById") Long createdById);

    // Métodos existentes mantidos para compatibilidade
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

    boolean existsByName(String name);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
