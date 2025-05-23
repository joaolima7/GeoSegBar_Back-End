package com.geosegbar.infra.user.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

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

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
