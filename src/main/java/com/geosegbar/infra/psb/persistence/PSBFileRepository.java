package com.geosegbar.infra.psb.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;

@Repository
public interface PSBFileRepository extends JpaRepository<PSBFileEntity, Long> {

    List<PSBFileEntity> findByPsbFolderOrderByUploadedAtDesc(PSBFolderEntity psbFolder);

    @EntityGraph(attributePaths = {"uploadedBy", "psbFolder"})
    List<PSBFileEntity> findByPsbFolderIdOrderByUploadedAtDesc(Long psbFolderId);

    @Override
    @EntityGraph(attributePaths = {"uploadedBy", "psbFolder"})
    Optional<PSBFileEntity> findById(Long id);
}
