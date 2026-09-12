package com.geosegbar.infra.checklist_response.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.WeatherConditionEnum;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.infra.dashboard.projections.CategoryCountProjection;
import com.geosegbar.infra.dashboard.projections.ChecklistResponseCountProjection;
import com.geosegbar.infra.dashboard.projections.DamResponseCountProjection;
import com.geosegbar.infra.checklist_response.projections.DamLastChecklistProjection;
import com.geosegbar.infra.mobile_dashboard.projections.DamInspectionProjection;
import com.geosegbar.infra.mobile_dashboard.projections.InspectionPaceProjection;
import com.geosegbar.infra.mobile_dashboard.projections.MonthlyCountProjection;

@Repository
public interface ChecklistResponseRepository extends JpaRepository<ChecklistResponseEntity, Long> {

    @EntityGraph(attributePaths = {
        "user",
        "dam",
        "questionnaireResponses",
        "questionnaireResponses.templateQuestionnaire",
        "questionnaireResponses.answers",
        "questionnaireResponses.answers.question",
        "questionnaireResponses.answers.question.options",
        "questionnaireResponses.answers.selectedOptions",
        "questionnaireResponses.answers.photos"
    })
    @Query("SELECT cr FROM ChecklistResponseEntity cr WHERE cr.dam.id = :damId ORDER BY cr.createdAt DESC")
    List<ChecklistResponseEntity> findByDamIdWithFullDetails(@Param("damId") Long damId);

    /**
     * Data da última inspeção de cada barragem do cliente, numa consulta só.
     *
     * A versão anterior percorria as barragens e, para cada uma, carregava
     * TODAS as respostas de checklist em memória só para tirar o max() em
     * Java. Aqui o LEFT JOIN mantém na resposta a barragem que nunca foi
     * inspecionada, com data nula — que é a informação que a tela precisa.
     */
    @Query(value = """
                        SELECT d.id            AS damId,
                               d.name          AS damName,
                               MAX(cr.created_at) AS lastChecklistDate
                        FROM dam d
                        LEFT JOIN checklist_responses cr ON cr.dam_id = d.id
                        WHERE d.client_id = :clientId
                        GROUP BY d.id, d.name
                        ORDER BY d.id ASC
                        """, nativeQuery = true)
    List<DamLastChecklistProjection> findLastChecklistDateByClient(@Param("clientId") Long clientId);

    @EntityGraph(attributePaths = {"user", "dam"})
    List<ChecklistResponseEntity> findByDamId(Long damId);

    @EntityGraph(attributePaths = {"user", "dam"})
    List<ChecklistResponseEntity> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "dam"})
    List<ChecklistResponseEntity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByChecklistId(Long checklistId);

    @EntityGraph(attributePaths = {"user", "dam"})
    @Query("SELECT cr FROM ChecklistResponseEntity cr WHERE cr.id = :id")
    Optional<ChecklistResponseEntity> findByIdWithBasicInfo(@Param("id") Long id);

    @EntityGraph(attributePaths = {
        "user",
        "dam",
        "questionnaireResponses",
        "questionnaireResponses.templateQuestionnaire",
        "questionnaireResponses.answers",
        "questionnaireResponses.answers.question",
        "questionnaireResponses.answers.selectedOptions",
        "questionnaireResponses.answers.photos"
    })
    @Query("SELECT cr FROM ChecklistResponseEntity cr WHERE cr.id = :id")
    Optional<ChecklistResponseEntity> findByIdWithFullDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "dam"})
    Page<ChecklistResponseEntity> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "dam"})
    Page<ChecklistResponseEntity> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "dam"})
    @Override
    Page<ChecklistResponseEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "dam"})
    @Query("SELECT cr FROM ChecklistResponseEntity cr WHERE cr.dam.id = :damId ORDER BY cr.createdAt DESC")
    Page<ChecklistResponseEntity> findByDamIdOptimized(@Param("damId") Long damId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "dam"})
    @Query("SELECT cr FROM ChecklistResponseEntity cr JOIN cr.dam d WHERE d.client.id = :clientId ORDER BY cr.createdAt DESC")
    Page<ChecklistResponseEntity> findByClientIdOptimized(@Param("clientId") Long clientId, Pageable pageable);

    @Query(value = """
    WITH client_dams AS (
        SELECT d.id AS dam_id
        FROM dam d
        WHERE d.client_id = :clientId
    ),
    latest_responses AS (
        SELECT cr.id, cr.checklist_id,
               ROW_NUMBER() OVER (PARTITION BY cr.checklist_id ORDER BY cr.created_at DESC) as row_num
        FROM checklist_responses cr
        JOIN client_dams cd ON cr.dam_id = cd.dam_id
    )
    SELECT id FROM latest_responses
    WHERE row_num <= :limit
    ORDER BY checklist_id, row_num
    """, nativeQuery = true)
    List<Long> findLatestChecklistResponseIdsByClientIdAndLimit(
            @Param("clientId") Long clientId,
            @Param("limit") int limit);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistResponseEntity cr SET "
            + "cr.upstreamLevel = COALESCE(:upstreamLevel, cr.upstreamLevel), "
            + "cr.downstreamLevel = COALESCE(:downstreamLevel, cr.downstreamLevel), "
            + "cr.spilledFlow = COALESCE(:spilledFlow, cr.spilledFlow), "
            + "cr.turbinedFlow = COALESCE(:turbinedFlow, cr.turbinedFlow), "
            + "cr.accumulatedRainfall = COALESCE(:accumulatedRainfall, cr.accumulatedRainfall), "
            + "cr.weatherCondition = COALESCE(:weatherCondition, cr.weatherCondition) "
            + "WHERE cr.id = :id")
    int updateTopLevelFields(
            @Param("id") Long id,
            @Param("upstreamLevel") Double upstreamLevel,
            @Param("downstreamLevel") Double downstreamLevel,
            @Param("spilledFlow") Double spilledFlow,
            @Param("turbinedFlow") Double turbinedFlow,
            @Param("accumulatedRainfall") Double accumulatedRainfall,
            @Param("weatherCondition") WeatherConditionEnum weatherCondition);

    boolean existsByUser_Id(Long userId);

    // ===================== Dashboard Queries =====================
    @Query("SELECT COUNT(cr) FROM ChecklistResponseEntity cr "
            + "WHERE cr.dam.id IN :damIds "
            + "AND cr.createdAt >= :startDate "
            + "AND cr.createdAt <= :endDate")
    long countByDamIdsAndDateRange(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(DISTINCT cr.user.id) FROM ChecklistResponseEntity cr "
            + "WHERE cr.dam.id IN :damIds "
            + "AND cr.createdAt >= :startDate "
            + "AND cr.createdAt <= :endDate")
    long countDistinctRespondents(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT cr.checklistId as checklistId, cr.checklistName as checklistName, COUNT(cr) as total "
            + "FROM ChecklistResponseEntity cr "
            + "WHERE cr.dam.id IN :damIds "
            + "AND cr.createdAt >= :startDate "
            + "AND cr.createdAt <= :endDate "
            + "GROUP BY cr.checklistId, cr.checklistName "
            + "ORDER BY total DESC")
    List<ChecklistResponseCountProjection> countByChecklistGrouped(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT CAST(cr.weatherCondition AS string) as name, COUNT(cr) as count "
            + "FROM ChecklistResponseEntity cr "
            + "WHERE cr.dam.id IN :damIds "
            + "AND cr.createdAt >= :startDate "
            + "AND cr.createdAt <= :endDate "
            + "AND cr.weatherCondition IS NOT NULL "
            + "GROUP BY cr.weatherCondition")
    List<CategoryCountProjection> countByWeatherConditionGrouped(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT cr.dam.id as damId, cr.dam.name as damName, COUNT(cr) as total "
            + "FROM ChecklistResponseEntity cr "
            + "WHERE cr.dam.id IN :damIds "
            + "AND cr.createdAt >= :startDate "
            + "AND cr.createdAt <= :endDate "
            + "GROUP BY cr.dam.id, cr.dam.name "
            + "ORDER BY total DESC")
    List<DamResponseCountProjection> countByDamGrouped(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ===================== Painel do aplicativo (/mobile/dashboard) =====================
    /**
     * Quantas inspeções ESTE usuário registrou, mês a mês, nas barragens que
     * ele acessa.
     *
     * Agrupa no banco: a resposta tem no máximo 12 linhas, uma por mês, em vez
     * das inspeções inteiras. Usa idx_checklist_response_user_created (user_id,
     * created_at), que cobre exatamente o filtro.
     *
     * Meses sem trabalho não aparecem aqui — quem densifica a série é o
     * serviço, para o gráfico de linha não interpolar buraco.
     */
    @Query(value = """
            SELECT to_char(cr.created_at, 'YYYY-MM') AS bucket,
                   CAST(COUNT(*) AS BIGINT) AS total
            FROM checklist_responses cr
            WHERE cr.user_id = :userId
              AND cr.dam_id IN (:damIds)
              AND cr.created_at >= :startDate
              AND cr.created_at <= :endDate
            GROUP BY to_char(cr.created_at, 'YYYY-MM')
            ORDER BY 1 ASC
            """, nativeQuery = true)
    List<MonthlyCountProjection> countMyResponsesByMonth(
            @Param("userId") Long userId,
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Uma linha por barragem acessível, inspecionada ou não.
     *
     * LEFT JOIN de propósito: a barragem que ninguém inspecionou precisa
     * aparecer com total zero e lastResponseAt nulo. É o que sustenta o "X de Y
     * barragens inspecionadas este mês" e o "nunca inspecionada" sem sentinela
     * de texto no campo de data.
     *
     * O FILTER conta só o período pedido, enquanto o MAX olha o histórico
     * inteiro — são perguntas diferentes ("quanto se inspecionou agora" e "há
     * quanto tempo esta barragem não é visitada") e seria desperdício resolver
     * cada uma numa consulta. Ambas caem em idx_checklist_response_dam_created_desc.
     */
    @Query(value = """
            SELECT d.id AS damId,
                   d.name AS damName,
                   CAST(COUNT(cr.id) FILTER (
                       WHERE cr.created_at >= :startDate AND cr.created_at <= :endDate
                   ) AS BIGINT) AS totalInPeriod,
                   MAX(cr.created_at) AS lastResponseAt
            FROM dam d
            LEFT JOIN checklist_responses cr ON cr.dam_id = d.id
            WHERE d.id IN (:damIds)
            GROUP BY d.id, d.name
            ORDER BY d.name ASC
            """, nativeQuery = true)
    List<DamInspectionProjection> findInspectionSummaryByDam(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * QUANTO TEMPO LEVA PARA PREENCHER UMA INSPECAO - nos tres recortes que o
     * app mostra, numa consulta so.
     *
     * GROUPING SETS em vez de tres consultas: os recortes sao a mesma
     * varredura vista de tres alturas (o cliente inteiro, cada barragem, cada
     * inspetor), e a mediana de um nivel nao se deduz do nivel de baixo -
     * mediana de medianas nao e mediana. Com GROUPING SETS o banco faz uma
     * passagem e devolve os tres; com tres @Query seriam tres varreduras do
     * mesmo indice para responder a mesma pergunta.
     *
     * O QUE ENTRA NA CONTA, e por que o filtro nao e frescura:
     *
     * started_at e finished_at sao cronometrados pelo APLICATIVO - o rascunho
     * marca quando o inspetor abriu e quando enviou. Tres situacoes precisam
     * ficar de fora, senao o numero deixa de significar "tempo em campo":
     *
     * 1. inspecao antiga, gravada antes de o app cronometrar (ponta nula);
     * 2. relogio do aparelho corrigido no meio (duracao negativa ou zero);
     * 3. RASCUNHO RETOMADO NO DIA SEGUINTE - o caso real e o que mais
     *    distorce: o inspetor abre a inspecao, o dia acaba, ele envia na
     *    manha seguinte. Sao 18 horas de duracao para 40 minutos de trabalho.
     *    O teto de 1 dia corta esse caso; ele nao corta jornada longa nenhuma,
     *    porque nenhuma inspecao de campo dura 24h.
     *
     * Por isso a resposta carrega TAMBEM quantas inspecoes entraram na conta
     * (inspections): o app diz "media de N inspecoes", e quem le sabe sobre
     * quantas o numero fala.
     *
     * MEDIANA E MEDIA, as duas. A media e o que se pede em voz alta; a mediana
     * e o que sobrevive a uma inspecao esquecida aberta por 20 horas dentro do
     * teto. Sao duas linhas de SQL na mesma passagem - deixar so uma seria
     * economizar no lugar errado.
     *
     * INDICE: o WHERE e (dam_id IN ..., created_at BETWEEN ...), que e
     * exatamente idx_checklist_response_dam_created_desc - o mesmo indice que
     * findInspectionSummaryByDam usa. Nenhum indice novo. Os dois JOIN sao por
     * chave primaria.
     *
     * O CAST PARA double precision e explicito de proposito: a partir do
     * PostgreSQL 14 EXTRACT(EPOCH FROM interval) devolve NUMERIC, e
     * percentile_cont nao tem sobrecarga para numeric - so para double
     * precision e interval. A resolucao funcionaria pelo cast implicito, mas
     * depender de cast implicito numa funcao de agregado ordenado e o tipo de
     * coisa que quebra numa atualizacao de versao, longe daqui.
     */
    @Query(value = """
            SELECT CASE
                       WHEN GROUPING(d.id) = 0 THEN 'DAM'
                       WHEN GROUPING(u.id) = 0 THEN 'USER'
                       ELSE 'ALL'
                   END AS scope,
                   COALESCE(d.id, u.id) AS groupId,
                   COALESCE(d.name, u.name) AS groupName,
                   CAST(COUNT(*) AS BIGINT) AS inspections,
                   CAST(ROUND(AVG(CAST(
                       EXTRACT(EPOCH FROM (cr.finished_at - cr.started_at))
                       AS double precision))) AS BIGINT) AS averageSeconds,
                   CAST(ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (
                       ORDER BY CAST(
                           EXTRACT(EPOCH FROM (cr.finished_at - cr.started_at))
                           AS double precision))) AS BIGINT) AS medianSeconds
            FROM checklist_responses cr
            JOIN dam d ON d.id = cr.dam_id
            JOIN users u ON u.id = cr.user_id
            WHERE cr.dam_id IN (:damIds)
              AND cr.created_at >= :startDate
              AND cr.created_at <= :endDate
              AND cr.started_at IS NOT NULL
              AND cr.finished_at IS NOT NULL
              AND cr.finished_at > cr.started_at
              AND cr.finished_at <= cr.started_at + INTERVAL '1 day'
            GROUP BY GROUPING SETS ((d.id, d.name), (u.id, u.name), ())
            """, nativeQuery = true)
    List<InspectionPaceProjection> findInspectionPace(
            @Param("damIds") List<Long> damIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
