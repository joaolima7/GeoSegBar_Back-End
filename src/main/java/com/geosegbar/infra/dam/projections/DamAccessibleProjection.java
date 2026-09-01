package com.geosegbar.infra.dam.projections;

/**
 * Projeção da barragem acessível, com os campos escalares que o app consome.
 *
 * Deliberadamente rasa: o app declara que usa 7 campos e descarta o resto, e
 * hoje recebe a DamEntity inteira com sections, reservoirs, psbFolders,
 * instruments e os @OneToOne — todos serializados e todos jogados fora.
 */
public interface DamAccessibleProjection {

    Long getDamId();

    String getDamName();

    String getStatus();

    Long getClientId();

    String getClientName();

    String getCity();

    String getState();

    Double getLatitude();

    Double getLongitude();
}
