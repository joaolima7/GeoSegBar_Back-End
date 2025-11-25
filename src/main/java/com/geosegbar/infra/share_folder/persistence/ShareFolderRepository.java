package com.geosegbar.infra.share_folder.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.ShareFolderEntity;
import com.geosegbar.entities.UserEntity;

@Repository
public interface ShareFolderRepository extends JpaRepository<ShareFolderEntity, Long> {

    List<ShareFolderEntity> findBySharedBy(UserEntity sharedBy);

    List<ShareFolderEntity> findBySharedWithEmailOrderByCreatedAtDesc(String email);

    List<ShareFolderEntity> findByPsbFolder(PSBFolderEntity psbFolder);

    Optional<ShareFolderEntity> findByToken(String token);

    List<ShareFolderEntity> findByPsbFolderIdAndSharedWithEmail(Long psbFolderId, String email);

    boolean existsByPsbFolderIdAndSharedWithEmail(Long psbFolderId, String email);

    @Query("SELECT s FROM ShareFolderEntity s WHERE s.psbFolder.dam.id = :damId ORDER BY s.createdAt DESC")
    List<ShareFolderEntity> findByPsbFolderDamIdOrderByCreatedAtDesc(@Param("damId") Long damId);

    @Query("SELECT s FROM ShareFolderEntity s "
            + "WHERE s.psbFolder.id = :psbFolderId "
            + "AND s.sharedWithEmail = :email "
            + "AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    List<ShareFolderEntity> findValidSharesByFolderAndEmail(
            @Param("psbFolderId") Long psbFolderId,
            @Param("email") String email,
            @Param("now") LocalDateTime now
    );
}
