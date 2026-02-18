package com.geosegbar.infra.psb.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.PSBFolderEntity;

@Repository
public interface PSBFolderRepository extends JpaRepository<PSBFolderEntity, Long> {

    @EntityGraph(attributePaths = {"dam"})
    List<PSBFolderEntity> findByDamOrderByFolderIndexAsc(DamEntity dam);

    @EntityGraph(attributePaths = {"dam"})
    List<PSBFolderEntity> findByDamIdOrderByFolderIndexAsc(Long damId);

    Optional<PSBFolderEntity> findByDamIdAndFolderIndex(Long damId, Integer folderIndex);

    boolean existsByDamIdAndName(Long damId, String name);

    boolean existsByDamIdAndFolderIndex(Long damId, Integer folderIndex);

    List<PSBFolderEntity> findByDamIdAndFolderIndexGreaterThanOrderByFolderIndexAsc(Long damId, Integer folderIndex);

    @EntityGraph(attributePaths = {"dam"})
    List<PSBFolderEntity> findByDamIdAndParentFolderIsNullOrderByFolderIndexAsc(Long damId);

    @EntityGraph(attributePaths = {"parentFolder", "parentFolder.dam"})
    List<PSBFolderEntity> findByParentFolderIdOrderByFolderIndexAsc(Long parentFolderId);

    boolean existsByDamIdAndNameAndParentFolderId(Long damId, String name, Long parentFolderId);

    boolean existsByDamIdAndNameAndParentFolderIsNull(Long damId, String name);

    boolean existsByParentFolderIdAndFolderIndex(Long parentFolderId, Integer folderIndex);

    boolean existsByDamIdAndFolderIndexAndParentFolderIsNull(Long damId, Integer folderIndex);

    List<PSBFolderEntity> findByParentFolderIdAndFolderIndexGreaterThanOrderByFolderIndexAsc(Long parentFolderId, Integer folderIndex);

    List<PSBFolderEntity> findByDamIdAndParentFolderIsNullAndFolderIndexGreaterThanOrderByFolderIndexAsc(Long damId, Integer folderIndex);

    @Query("SELECT DISTINCT f FROM PSBFolderEntity f "
            + "LEFT JOIN FETCH f.files "
            + "LEFT JOIN FETCH f.subfolders "
            + "WHERE f.dam.id = :damId AND f.parentFolder IS NULL "
            + "ORDER BY f.folderIndex ASC")
    List<PSBFolderEntity> findCompleteHierarchyByDamId(@Param("damId") Long damId);

    @Override
    @EntityGraph(attributePaths = {"dam", "parentFolder"})
    Optional<PSBFolderEntity> findById(Long id);

    /**
     * Query enxuta: retorna APENAS o serverPath, sem carregar a entidade
     * inteira nem o EntityGraph (dam, parentFolder, etc.). Usado pelo upload
     * para evitar N+1 queries desnecessárias.
     */
    @Query("SELECT f.serverPath FROM PSBFolderEntity f WHERE f.id = :folderId")
    Optional<String> findServerPathById(@Param("folderId") Long folderId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM PSBFolderEntity f WHERE f.id = :folderId")
    boolean existsFolderById(@Param("folderId") Long folderId);
}
