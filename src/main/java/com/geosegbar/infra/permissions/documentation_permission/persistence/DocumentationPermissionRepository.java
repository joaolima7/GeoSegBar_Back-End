package com.geosegbar.infra.permissions.documentation_permission.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DocumentationPermissionEntity;
import com.geosegbar.entities.UserEntity;

@Repository
public interface DocumentationPermissionRepository extends JpaRepository<DocumentationPermissionEntity, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<DocumentationPermissionEntity> findByUser(UserEntity user);

    boolean existsByUser(UserEntity user);

    void deleteByUser(UserEntity user);
}
