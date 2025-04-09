package com.geosegbar.infra.psb.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;

@Repository
public interface PSBFileRepository extends JpaRepository<PSBFileEntity, Long> {
    List<PSBFileEntity> findByPsbFolderOrderByUploadedAtDesc(PSBFolderEntity psbFolder);
    List<PSBFileEntity> findByPsbFolderIdOrderByUploadedAtDesc(Long psbFolderId);
}