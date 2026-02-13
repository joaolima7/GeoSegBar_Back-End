package com.geosegbar.infra.documentation_dam.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DocumentationDamEntity;
import com.geosegbar.infra.documentation_dam.dtos.DocumentationDamResponseDTO;

@Repository
public interface DocumentationDamRepository extends JpaRepository<DocumentationDamEntity, Long> {

    @EntityGraph(attributePaths = {"dam"})
    Optional<DocumentationDamEntity> findByDam(DamEntity dam);

    @EntityGraph(attributePaths = {"dam"})
    Optional<DocumentationDamEntity> findByDamId(Long damId);

    @Override
    @EntityGraph(attributePaths = {"dam"})
    List<DocumentationDamEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"dam"})
    Optional<DocumentationDamEntity> findById(Long id);

    boolean existsByDam(DamEntity dam);

    boolean existsByDamId(Long damId);

    /**
     * Query performática que projeta apenas os campos necessários sem carregar
     * a entidade Dam. Usado para listagens onde não é necessário carregar toda
     * a informação da Dam.
     */
    @Query("SELECT new com.geosegbar.infra.documentation_dam.dtos.DocumentationDamResponseDTO("
            + "d.id, d.dam.id, d.lastUpdatePAE, d.nextUpdatePAE, d.lastUpdatePSB, d.nextUpdatePSB, "
            + "d.lastUpdateRPSB, d.nextUpdateRPSB, d.lastAchievementISR, d.nextAchievementISR, "
            + "d.lastAchievementChecklist, d.nextAchievementChecklist, d.lastFillingFSB, d.nextFillingFSB, "
            + "d.lastInternalSimulation, d.nextInternalSimulation, d.lastExternalSimulation, d.nextExternalSimulation) "
            + "FROM DocumentationDamEntity d "
            + "ORDER BY d.id ASC")
    List<DocumentationDamResponseDTO> findAllLightweight();

    /**
     * Query performática que retorna um único registro sem carregar a entidade
     * Dam completa.
     */
    @Query("SELECT new com.geosegbar.infra.documentation_dam.dtos.DocumentationDamResponseDTO("
            + "d.id, d.dam.id, d.lastUpdatePAE, d.nextUpdatePAE, d.lastUpdatePSB, d.nextUpdatePSB, "
            + "d.lastUpdateRPSB, d.nextUpdateRPSB, d.lastAchievementISR, d.nextAchievementISR, "
            + "d.lastAchievementChecklist, d.nextAchievementChecklist, d.lastFillingFSB, d.nextFillingFSB, "
            + "d.lastInternalSimulation, d.nextInternalSimulation, d.lastExternalSimulation, d.nextExternalSimulation) "
            + "FROM DocumentationDamEntity d "
            + "WHERE d.id = :id")
    Optional<DocumentationDamResponseDTO> findByIdLightweight(@Param("id") Long id);

    /**
     * Query performática que retorna documentação por damId sem carregar a
     * entidade Dam completa.
     */
    @Query("SELECT new com.geosegbar.infra.documentation_dam.dtos.DocumentationDamResponseDTO("
            + "d.id, d.dam.id, d.lastUpdatePAE, d.nextUpdatePAE, d.lastUpdatePSB, d.nextUpdatePSB, "
            + "d.lastUpdateRPSB, d.nextUpdateRPSB, d.lastAchievementISR, d.nextAchievementISR, "
            + "d.lastAchievementChecklist, d.nextAchievementChecklist, d.lastFillingFSB, d.nextFillingFSB, "
            + "d.lastInternalSimulation, d.nextInternalSimulation, d.lastExternalSimulation, d.nextExternalSimulation) "
            + "FROM DocumentationDamEntity d "
            + "WHERE d.dam.id = :damId")
    Optional<DocumentationDamResponseDTO> findByDamIdLightweight(@Param("damId") Long damId);
}
