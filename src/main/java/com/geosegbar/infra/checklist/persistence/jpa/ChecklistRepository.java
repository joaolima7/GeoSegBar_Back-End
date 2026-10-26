package com.geosegbar.infra.checklist.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.ChecklistEntity;

@Repository
public interface ChecklistRepository extends JpaRepository<ChecklistEntity, Long> {

    @EntityGraph(attributePaths = {"checklistTemplates", "checklistTemplates.templateQuestionnaire"})
    ChecklistEntity findByDamId(Long damId);

    @EntityGraph(attributePaths = {
        "checklistTemplates",
        "checklistTemplates.templateQuestionnaire",
        "dam"
    })
    @Query("SELECT DISTINCT c FROM ChecklistEntity c WHERE c.dam.id = :damId")
    ChecklistEntity findByDamIdWithFullDetails(@Param("damId") Long damId);

    @EntityGraph(attributePaths = {
        "checklistTemplates",
        "checklistTemplates.templateQuestionnaire",
        "dam",
        "dam.client"
    })
    @Query("SELECT DISTINCT c FROM ChecklistEntity c WHERE c.id = :id")
    Optional<ChecklistEntity> findByIdWithFullDetails(@Param("id") Long id);

    /**
     * Estrutura dos checklists para o payload "com últimas respostas": uma linha
     * por pergunta de cada questionário de cada checklist. Checklist sem
     * questionário vem uma vez, com as colunas do questionário nulas; questionário
     * sem pergunta, idem com as da pergunta. Usada por
     * ChecklistService.findChecklistsWithLastAnswersForDam e
     * findAllChecklistsWithLastAnswersByClientId.
     *
     * Substitui o carregamento por entidade, que custava uma query por pergunta
     * (última resposta) mais as relações EAGER de cada barragem: no cliente 18,
     * 368 queries e até 1,5 s. Esta leva ~12 ms em produção (26/10/2026,
     * cliente 18, 512 perguntas).
     *
     * Colunas (aliases únicos: o Hibernate recusa alias repetido em consulta
     * nativa): 0 checklist_id, 1 checklist_name, 2 checklist_created_at,
     * 3 dam_id, 4 dam_name, 5 dam_city, 6 dam_state, 7 dam_latitude,
     * 8 dam_longitude, 9 checklist_template_id, 10 template_order_index,
     * 11 template_id, 12 template_name, 13 question_order_index,
     * 14 question_id, 15 question_text, 16 question_type.
     */
    String LAST_ANSWERS_STRUCTURE = """
            SELECT c.id AS checklist_id,
                   c.name AS checklist_name,
                   c.created_at AS checklist_created_at,
                   d.id AS dam_id,
                   d.name AS dam_name,
                   d.city AS dam_city,
                   d.state AS dam_state,
                   d.latitude AS dam_latitude,
                   d.longitude AS dam_longitude,
                   ct.id AS checklist_template_id,
                   ct.order_index AS template_order_index,
                   t.id AS template_id,
                   t.name AS template_name,
                   tqq.order_index AS question_order_index,
                   q.id AS question_id,
                   q.question_text AS question_text,
                   q.type AS question_type
            FROM checklists c
            INNER JOIN dam d ON d.id = c.dam_id
            LEFT JOIN checklist_templates ct ON ct.checklist_id = c.id
            LEFT JOIN template_questionnaires t ON t.id = ct.template_questionnaire_id
            LEFT JOIN template_questionnaire_questions tqq ON tqq.template_questionnaire_id = t.id
            LEFT JOIN questions q ON q.id = tqq.question_id
            """;

    String LAST_ANSWERS_STRUCTURE_ORDER = """
            ORDER BY c.id, ct.order_index, ct.id, tqq.order_index, tqq.id
            """;

    @Query(value = LAST_ANSWERS_STRUCTURE + "WHERE c.dam_id = :damId\n" + LAST_ANSWERS_STRUCTURE_ORDER,
            nativeQuery = true)
    List<Object[]> findLastAnswersStructureByDamId(@Param("damId") Long damId);

    @Query(value = LAST_ANSWERS_STRUCTURE + "WHERE d.client_id = :clientId\n" + LAST_ANSWERS_STRUCTURE_ORDER,
            nativeQuery = true)
    List<Object[]> findLastAnswersStructureByClientId(@Param("clientId") Long clientId);

    @EntityGraph(attributePaths = {"dam"})
    @Query("SELECT c FROM ChecklistEntity c WHERE c.id = :checklistId")
    Optional<ChecklistEntity> findByIdWithDam(@Param("checklistId") Long checklistId);

    @EntityGraph(attributePaths = {"checklistTemplates", "checklistTemplates.templateQuestionnaire", "dam"})
    @Query("SELECT c FROM ChecklistEntity c WHERE c.id = :checklistId")
    Optional<ChecklistEntity> findByIdWithTemplates(@Param("checklistId") Long checklistId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByNameAndDamId(String name, Long damId);

    boolean existsByNameAndDamIdAndIdNot(String name, Long damId, Long id);

    @Query("SELECT COUNT(ct) FROM ChecklistTemplateEntity ct WHERE ct.templateQuestionnaire.id = :templateId")
    long countByTemplateId(@Param("templateId") Long templateId);

    @Query(value = "SELECT c FROM ChecklistEntity c JOIN FETCH c.dam",
            countQuery = "SELECT count(c) FROM ChecklistEntity c")
    Page<ChecklistEntity> findAllWithDams(Pageable pageable);

    @Query("SELECT COUNT(c) FROM ChecklistEntity c WHERE c.dam.id IN :damIds")
    long countByDamIds(@Param("damIds") List<Long> damIds);
}
