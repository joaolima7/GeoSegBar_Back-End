package com.geosegbar.infra.mobile_dashboard.projections;

/**
 * Uma barra da série temporal: o mês no formato "YYYY-MM" e o total daquele
 * mês. O agrupamento é feito no banco — o app recebe no máximo 12 linhas.
 */
public interface MonthlyCountProjection {

    String getBucket();

    Long getTotal();
}
