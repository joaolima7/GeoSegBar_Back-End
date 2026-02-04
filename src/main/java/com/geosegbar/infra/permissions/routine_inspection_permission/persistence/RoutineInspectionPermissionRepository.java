package com.geosegbar.infra.permissions.routine_inspection_permission.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.RoutineInspectionPermissionEntity;
import com.geosegbar.entities.UserEntity;

@Repository
public interface RoutineInspectionPermissionRepository extends JpaRepository<RoutineInspectionPermissionEntity, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<RoutineInspectionPermissionEntity> findByUser(UserEntity user);

    boolean existsByUser(UserEntity user);

    void deleteByUser(UserEntity user);

    @Override
    @EntityGraph(attributePaths = {"user"})
    List<RoutineInspectionPermissionEntity> findAll();
}
