package com.geosegbar.infra.instrument_graph_axes.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.InstrumentGraphAxesEntity;

@Repository
public interface InstrumentGraphAxesRepository extends JpaRepository<InstrumentGraphAxesEntity, Long> {

    Optional<InstrumentGraphAxesEntity> findByPatternId(Long patternId);

    @Query("SELECT a FROM InstrumentGraphAxesEntity a "
            + "WHERE a.pattern.instrument.id = :instrumentId")
    Optional<InstrumentGraphAxesEntity> findByPatternInstrumentId(@Param("instrumentId") Long instrumentId);

    @Query("SELECT a FROM InstrumentGraphAxesEntity a "
            + "WHERE a.pattern.id = :patternId AND a.abscissaGridLinesEnable = true")
    Optional<InstrumentGraphAxesEntity> findByPatternIdAndAbscissaGridLinesEnabled(@Param("patternId") Long patternId);

    @Query("SELECT a FROM InstrumentGraphAxesEntity a "
            + "WHERE a.pattern.id = :patternId AND a.primaryOrdinateGridLinesEnable = true")
    Optional<InstrumentGraphAxesEntity> findByPatternIdAndPrimaryOrdinateGridLinesEnabled(@Param("patternId") Long patternId);

    @Query("SELECT a FROM InstrumentGraphAxesEntity a "
            + "WHERE a.abscissaPx = :abscissaPx AND a.primaryOrdinatePx = :primaryOrdinatePx")
    Optional<InstrumentGraphAxesEntity> findByFontSizes(@Param("abscissaPx") Integer abscissaPx,
            @Param("primaryOrdinatePx") Integer primaryOrdinatePx);

    @Query("SELECT a FROM InstrumentGraphAxesEntity a "
            + "WHERE a.pattern.id = :patternId "
            + "AND (a.primaryOrdinateInitialValue IS NOT NULL OR a.secondaryOrdinateInitialValue IS NOT NULL)")
    Optional<InstrumentGraphAxesEntity> findByPatternIdWithInitialValues(@Param("patternId") Long patternId);

    @Query("SELECT a FROM InstrumentGraphAxesEntity a "
            + "WHERE a.pattern.id = :patternId "
            + "AND (a.primaryOrdinateMaximumValue IS NOT NULL OR a.secondaryOrdinateMaximumValue IS NOT NULL)")
    Optional<InstrumentGraphAxesEntity> findByPatternIdWithMaximumValues(@Param("patternId") Long patternId);
}
