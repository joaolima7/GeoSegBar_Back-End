package com.geosegbar.infra.mobile_dashboard.projections;

import java.time.LocalDateTime;

/**
 * Uma barragem acessível e o estado de inspeção dela.
 *
 * Vem de LEFT JOIN: barragem sem nenhuma inspeção também aparece, com
 * totalInPeriod = 0 e lastResponseAt nulo. É o que permite dizer "nunca
 * inspecionada" sem sentinela de texto no campo de data.
 */
public interface DamInspectionProjection {

    Long getDamId();

    String getDamName();

    Long getTotalInPeriod();

    LocalDateTime getLastResponseAt();
}
