package com.geosegbar.infra.psb.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.PSBFolderEntity;

@Repository
public interface PSBFolderRepository extends JpaRepository<PSBFolderEntity, Long> {
    List<PSBFolderEntity> findByDamOrderByFolderIndexAsc(DamEntity dam);
    List<PSBFolderEntity> findByDamIdOrderByFolderIndexAsc(Long damId);
    Optional<PSBFolderEntity> findByDamIdAndFolderIndex(Long damId, Integer folderIndex);
    boolean existsByDamIdAndName(Long damId, String name);
    boolean existsByDamIdAndFolderIndex(Long damId, Integer folderIndex);
}