package com.geosegbar.infra.permissions.atributions_permission.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AttributionsPermissionEntity;
import com.geosegbar.entities.UserEntity;

@Repository
public interface AttributionsPermissionRepository extends JpaRepository<AttributionsPermissionEntity, Long> {
    
    Optional<AttributionsPermissionEntity> findByUser(UserEntity user);
    
    boolean existsByUser(UserEntity user);
    
    void deleteByUser(UserEntity user);
}