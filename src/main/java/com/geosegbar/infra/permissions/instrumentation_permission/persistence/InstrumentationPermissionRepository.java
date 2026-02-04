package com.geosegbar.infra.permissions.instrumentation_permission.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentationPermissionEntity;
import com.geosegbar.entities.UserEntity;

@Repository
public interface InstrumentationPermissionRepository extends JpaRepository<InstrumentationPermissionEntity, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<InstrumentationPermissionEntity> findByUser(UserEntity user);

    boolean existsByUser(UserEntity user);

    void deleteByUser(UserEntity user);

    @Override
    @EntityGraph(attributePaths = {"user"})
    List<InstrumentationPermissionEntity> findAll();
}
