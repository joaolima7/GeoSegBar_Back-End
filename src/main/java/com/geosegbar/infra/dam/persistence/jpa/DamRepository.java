package com.geosegbar.infra.dam.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.infra.dam.projections.DamAccessibleProjection;
import com.geosegbar.infra.dam.projections.DamQuickAccessProjection;

@Repository
public interface DamRepository extends JpaRepository<DamEntity, Long> {

    @Override
    @EntityGraph(attributePaths = {"client", "status"})
    Optional<DamEntity> findById(Long id);

    @EntityGraph(attributePaths = {"client", "status"})
    List<DamEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"client", "status"})
    List<DamEntity> findByClientId(Long clientId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByNameAndClientId(String name, Long clientId);

    boolean existsByNameAndClientIdAndIdNot(String name, Long clientId, Long id);

    @Modifying
    @Query("UPDATE DamEntity d SET d.status = :status WHERE d.client.id = :clientId")
    int updateStatusByClientId(@Param("clientId") Long clientId, @Param("status") StatusEntity status);

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "LEFT JOIN FETCH d.client c "
            + "LEFT JOIN FETCH c.status "
            + "LEFT JOIN FETCH d.status "
            + "LEFT JOIN FETCH d.regulatoryDam rd "
            + "LEFT JOIN FETCH rd.securityLevel "
            + "LEFT JOIN FETCH rd.riskCategory "
            + "LEFT JOIN FETCH rd.potentialDamage "
            + "LEFT JOIN FETCH rd.classificationDam "
            + "LEFT JOIN FETCH d.documentationDam "
            + "ORDER BY d.id ASC")
    List<DamEntity> findAllComplete();

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "LEFT JOIN FETCH d.client c "
            + "LEFT JOIN FETCH c.status "
            + "LEFT JOIN FETCH d.status "
            + "LEFT JOIN FETCH d.regulatoryDam rd "
            + "LEFT JOIN FETCH rd.securityLevel "
            + "LEFT JOIN FETCH rd.riskCategory "
            + "LEFT JOIN FETCH rd.potentialDamage "
            + "LEFT JOIN FETCH rd.classificationDam "
            + "LEFT JOIN FETCH d.documentationDam "
            + "WHERE (:clientId IS NULL OR d.client.id = :clientId) "
            + "AND (:statusId IS NULL OR d.status.id = :statusId) "
            + "ORDER BY d.id ASC")
    List<DamEntity> findByClientAndStatusComplete(
            @Param("clientId") Long clientId,
            @Param("statusId") Long statusId);

    @Query("SELECT DISTINCT d FROM DamEntity d "
            + "LEFT JOIN FETCH d.client c "
            + "LEFT JOIN FETCH c.status "
            + "LEFT JOIN FETCH d.status "
            + "LEFT JOIN FETCH d.regulatoryDam rd "
            + "LEFT JOIN FETCH rd.securityLevel "
            + "LEFT JOIN FETCH rd.riskCategory "
            + "LEFT JOIN FETCH rd.potentialDamage "
            + "LEFT JOIN FETCH rd.classificationDam "
            + "LEFT JOIN FETCH d.documentationDam "
            + "WHERE d.id = :id")
    Optional<DamEntity> findByIdComplete(@Param("id") Long id);

    @Query(value = """
                        SELECT
                                d.id AS damId,
                                d.name AS damName,
                                s.status AS status,
                                c.id AS clientId,
                                c.name AS clientName
                        FROM dam d
                        INNER JOIN status s ON s.id = d.status_id
                        INNER JOIN client c ON c.id = d.client_id
                        ORDER BY d.id ASC
                        """, nativeQuery = true)
    List<DamQuickAccessProjection> findAllQuickAccess();

    /**
     * Ids das barragens que o usuário pode acessar. Mesma regra do
     * findQuickAccessByUserId — associação com o cliente E permissão com
     * has_access — só que devolvendo id, para quem precisa apenas do recorte.
     */
    @Query(value = """
                        SELECT DISTINCT d.id
                        FROM dam_permissions dp
                        INNER JOIN dam d ON d.id = dp.dam_id
                        INNER JOIN user_client uc ON uc.client_id = d.client_id
                        WHERE dp.user_id = :userId
                          AND uc.user_id = :userId
                          AND dp.has_access = true
                        ORDER BY d.id ASC
                        """, nativeQuery = true)
    List<Long> findAccessibleDamIdsByUserId(@Param("userId") Long userId);

    /**
     * Atalho de ADMIN: todas as barragens.
     */
    @Query(value = "SELECT d.id FROM dam d ORDER BY d.id ASC", nativeQuery = true)
    List<Long> findAllDamIds();

    /**
     * Barragens acessíveis com os 9 campos que o app usa: os 5 do
     * quick-access mais city, state, latitude e longitude.
     *
     * O app pede um 10º campo, "acronym", que NÃO EXISTE em barragem — nem na
     * entidade nem na tabela. Existe sigla de constante, de input e de output
     * de instrumento, nunca de barragem. O DTO Dart declara o campo e sempre
     * recebeu nulo; criar a coluna agora entregaria nulo para as 849 barragens
     * do mesmo jeito, só que com migração no meio.
     *
     * Rota nova em vez de ampliar o DamQuickAccessDTO, que a web consome.
     */
    @Query(value = """
                        SELECT DISTINCT
                                d.id AS damId,
                                d.name AS damName,
                                s.status AS status,
                                c.id AS clientId,
                                c.name AS clientName,
                                d.city AS city,
                                d.state AS state,
                                d.latitude AS latitude,
                                d.longitude AS longitude
                        FROM dam_permissions dp
                        INNER JOIN dam d ON d.id = dp.dam_id
                        INNER JOIN status s ON s.id = d.status_id
                        INNER JOIN client c ON c.id = d.client_id
                        INNER JOIN user_client uc ON uc.client_id = c.id
                        WHERE dp.user_id = :userId
                          AND uc.user_id = :userId
                          AND dp.has_access = true
                        ORDER BY d.id ASC
                        """, nativeQuery = true)
    List<DamAccessibleProjection> findAccessibleByUserId(@Param("userId") Long userId);

    @Query(value = """
                        SELECT
                                d.id AS damId,
                                d.name AS damName,
                                s.status AS status,
                                c.id AS clientId,
                                c.name AS clientName,
                                d.city AS city,
                                d.state AS state,
                                d.latitude AS latitude,
                                d.longitude AS longitude
                        FROM dam d
                        INNER JOIN status s ON s.id = d.status_id
                        INNER JOIN client c ON c.id = d.client_id
                        ORDER BY d.id ASC
                        """, nativeQuery = true)
    List<DamAccessibleProjection> findAllAccessible();

    @Query(value = """
                        SELECT DISTINCT
                                d.id AS damId,
                                d.name AS damName,
                                s.status AS status,
                                c.id AS clientId,
                                c.name AS clientName
                        FROM dam_permissions dp
                        INNER JOIN dam d ON d.id = dp.dam_id
                        INNER JOIN status s ON s.id = d.status_id
                        INNER JOIN client c ON c.id = d.client_id
                        INNER JOIN user_client uc ON uc.client_id = c.id
                        WHERE dp.user_id = :userId
                          AND uc.user_id = :userId
                          AND dp.has_access = true
                        ORDER BY d.id ASC
                        """, nativeQuery = true)
    List<DamQuickAccessProjection> findQuickAccessByUserId(@Param("userId") Long userId);
}
