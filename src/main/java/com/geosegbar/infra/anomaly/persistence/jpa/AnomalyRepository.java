package com.geosegbar.infra.anomaly.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.infra.dashboard.projections.CategoryCountProjection;
import com.geosegbar.infra.dashboard.projections.RecentAnomalyProjection;

@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEntity, Long> {

    List<AnomalyEntity> findByChecklistResponseIdOrderByIdAsc(Long checklistResponseId);

    List<AnomalyEntity> findByChecklistResponseIdInOrderByIdAsc(List<Long> checklistResponseIds);


    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findByDamIdAndUserId(Long damId, Long userId);

    @Override
    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    Optional<AnomalyEntity> findById(Long id);

    @EntityGraph(attributePaths = {"user", "dam", "dam.client", "photos", "dangerLevel", "status"})
    List<AnomalyEntity> findByDamId(Long damId);

    /**
     * Anomalias filtradas e paginadas.
     *
     * A única listagem fora do dashboard era findByDamId: TODAS as anomalias
     * da barragem, sem filtro, sem período e sem paginação — impraticável em
     * aparelho. Os parâmetros nulos são ignorados, então a mesma consulta
     * serve para qualquer combinação.
     *
     * O EntityGraph traz as associações EAGER numa consulta só; sem ele o
     * Hibernate dispara um SELECT por associação por linha.
     *
     * "photos" fica DE FORA de propósito, ao contrário das consultas vizinhas:
     * é coleção, e coleção com paginação faz o Hibernate trazer o resultado
     * inteiro e paginar em memória. Numa rota que existe justamente para
     * paginar, isso anularia o motivo dela existir.
     */
    @EntityGraph(attributePaths = {"user", "dam", "dangerLevel", "status"})
    @Query("""
           SELECT a FROM AnomalyEntity a
           WHERE a.dam.id IN :damIds
             AND (:statusId IS NULL OR a.status.id = :statusId)
             AND (CAST(:startDate AS timestamp) IS NULL OR a.createdAt >= :startDate)
             AND (CAST(:endDate AS timestamp) IS NULL OR a.createdAt <= :endDate)
           ORDER BY a.createdAt DESC
           """)
    Page<AnomalyEntity> findByFilters(
            @Param("damIds") List<Long> damIds,
            @Param("statusId") Long statusId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // ===================== Dashboard Queries =====================
    @Query("SELECT dl.name AS name, COUNT(a) AS count "
            + "FROM AnomalyEntity a JOIN a.dangerLevel dl "
            + "WHERE a.dam.id IN :damIds "
            + "AND a.createdAt >= :startDate "
            + "AND a.createdAt <= :endDate "
            + "GROUP BY dl.name")
    List<CategoryCountProjection> countByDangerLevelGrouped(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT s.name AS name, COUNT(a) AS count "
            + "FROM AnomalyEntity a JOIN a.status s "
            + "WHERE a.dam.id IN :damIds "
            + "AND a.createdAt >= :startDate "
            + "AND a.createdAt <= :endDate "
            + "GROUP BY s.name")
    List<CategoryCountProjection> countByStatusGrouped(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            SELECT
                a.id as id,
                u.id as userId,
                u.name as userName,
                d.id as damId,
                d.name as damName,
                a.created_at as createdAt,
                a.latitude as latitude,
                a.longitude as longitude,
                a.origin as origin,
                a.observation as observation,
                a.recommendation as recommendation,
                dl.name as dangerLevelName,
                ast.name as statusName
            FROM anomalies a
            INNER JOIN users u ON a.user_id = u.id
            INNER JOIN dam d ON a.dam_id = d.id
            INNER JOIN danger_levels dl ON a.danger_level_id = dl.id
            INNER JOIN anomaly_status ast ON a.status_id = ast.id
            WHERE a.dam_id IN (:damIds)
            ORDER BY a.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<RecentAnomalyProjection> findRecentByDamIds(
            @Param("damIds") List<Long> damIds,
            @Param("limit") int limit);

    boolean existsByUser_Id(Long userId);

    /**
     * Fotos da resposta mais recente, com foto, ao ponto de cada anomalia de
     * checklist da barragem. O ponto é (barragem, questionário modelo,
     * pergunta) — o mesmo que define a resposta anterior no preenchimento.
     *
     * A janela começa na inspeção que identificou a anomalia e termina antes
     * da próxima anomalia do mesmo ponto: depois de um NE e de um novo PV, as
     * fotos passam a ser da anomalia nova, não da antiga.
     *
     * A ordem segue a data da inspeção (checklist_responses.created_at), que é
     * editável, e não o instante em que a resposta foi gravada.
     */
    @Query(value = """
            SELECT an.id AS anomalyId,
                   latest.checklist_response_id AS checklistResponseId,
                   latest.inspected_at AS photoDate,
                   ap.image_path AS imagePath
            FROM anomalies an
            CROSS JOIN LATERAL (
                SELECT a.id AS answer_id,
                       cr.id AS checklist_response_id,
                       cr.created_at AS inspected_at
                FROM questionnaire_responses qr
                JOIN checklist_responses cr ON cr.id = qr.checklist_response_id
                JOIN answers a ON a.questionnaire_response_id = qr.id
                              AND a.question_id = an.question_id
                WHERE qr.dam_id = an.dam_id
                  AND qr.template_questionnaire_id = an.questionnaire_id
                  AND (an.created_at IS NULL OR cr.created_at >= an.created_at)
                  AND EXISTS (SELECT 1 FROM answer_photos p WHERE p.answer_id = a.id)
                  AND NOT EXISTS (
                      SELECT 1 FROM anomalies nx
                      WHERE nx.dam_id = an.dam_id
                        AND nx.question_id = an.question_id
                        AND nx.questionnaire_id = an.questionnaire_id
                        AND nx.id <> an.id
                        AND nx.created_at > an.created_at
                        AND nx.created_at <= cr.created_at)
                ORDER BY cr.created_at DESC, cr.id DESC, a.id DESC
                LIMIT 1
            ) latest
            JOIN answer_photos ap ON ap.answer_id = latest.answer_id
            WHERE an.dam_id = :damId
              AND an.question_id IS NOT NULL
              AND an.questionnaire_id IS NOT NULL
            ORDER BY an.id, ap.id
            """, nativeQuery = true)
    List<AnomalyLatestPhotoProjection> findLatestPhotosByDamId(@Param("damId") Long damId);

    @Query(value = """
            SELECT a.id, a.latitude, a.longitude,
                   a.question_id AS questionId,
                   q.question_text AS questionText,
                   a.questionnaire_id AS questionnaireId,
                   tq.name AS questionnaireName,
                   dl.name AS dangerLevelName,
                   ast.name AS statusName
            FROM anomalies a
            LEFT JOIN questions q ON q.id = a.question_id
            LEFT JOIN template_questionnaires tq ON tq.id = a.questionnaire_id
            JOIN danger_levels dl ON dl.id = a.danger_level_id
            JOIN anomaly_status ast ON ast.id = a.status_id
            WHERE a.dam_id = :damId
            AND ast.name != 'Concluído'
            """, nativeQuery = true)
    List<Object[]> findMapDataByDamId(@Param("damId") Long damId);
}
