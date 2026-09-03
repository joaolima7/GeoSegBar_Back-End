package com.geosegbar.infra.mobile_dashboard.projections;

import java.time.LocalDate;

/**
 * Instrumento cuja última leitura ficou fora do normal. O que o inspetor
 * precisa ver no topo do painel e alcançar em um toque.
 */
public interface CriticalInstrumentProjection {

    Long getInstrumentId();

    String getInstrumentName();

    Long getDamId();

    String getDamName();

    String getLimitStatus();

    LocalDate getLastReadingDate();
}
