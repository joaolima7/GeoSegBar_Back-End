package com.geosegbar.infra.mobile_dashboard.projections;

/**
 * Quanto tempo uma inspeção leva, agregado em três recortes de uma vez.
 *
 * A consulta usa GROUPING SETS, então cada linha traz o recorte a que
 * pertence:
 *
 * ALL   - o cliente inteiro (as barragens acessíveis), groupId e groupName
 *         nulos; DAM   - uma barragem; USER  - um inspetor.
 *
 * averageSeconds e medianSeconds vêm em SEGUNDOS, e nunca são nulos numa
 * linha que existe: uma linha só nasce se houve ao menos uma inspeção com as
 * duas pontas cronometradas.
 */
public interface InspectionPaceProjection {

    String getScope();

    Long getGroupId();

    String getGroupName();

    Long getInspections();

    Long getAverageSeconds();

    Long getMedianSeconds();
}
