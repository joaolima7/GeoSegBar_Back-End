package com.geosegbar.infra.instrument_graph_customization_properties.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;
import com.geosegbar.entities.InstrumentGraphCustomizationPropertiesEntity;

@Repository
public interface InstrumentGraphCustomizationPropertiesRepository extends JpaRepository<InstrumentGraphCustomizationPropertiesEntity, Long> {

    List<InstrumentGraphCustomizationPropertiesEntity> findByPatternId(Long patternId);

    List<InstrumentGraphCustomizationPropertiesEntity> findByCustomizationType(CustomizationTypeEnum customizationType);

    List<InstrumentGraphCustomizationPropertiesEntity> findByPatternIdAndCustomizationType(
            Long patternId, CustomizationTypeEnum customizationType);

    Optional<InstrumentGraphCustomizationPropertiesEntity> findByNameAndPatternId(String name, Long patternId);

    List<InstrumentGraphCustomizationPropertiesEntity> findByOutputId(Long outputId);

    List<InstrumentGraphCustomizationPropertiesEntity> findByStatisticalLimitId(Long statisticalLimitId);

    List<InstrumentGraphCustomizationPropertiesEntity> findByDeterministicLimitId(Long deterministicLimitId);

    List<InstrumentGraphCustomizationPropertiesEntity> findByInstrumentId(Long instrumentId);

    @Query("SELECT p FROM InstrumentGraphCustomizationPropertiesEntity p "
            + "WHERE p.customizationType = 'LINIMETRIC_RULER' AND p.pattern.id = :patternId")
    Optional<InstrumentGraphCustomizationPropertiesEntity> findLinimetricRulerByPatternId(@Param("patternId") Long patternId);

    List<InstrumentGraphCustomizationPropertiesEntity> findByLabelEnableTrue();

    List<InstrumentGraphCustomizationPropertiesEntity> findByIsPrimaryOrdinateTrue();

    List<InstrumentGraphCustomizationPropertiesEntity> findByIsPrimaryOrdinateFalse();

    List<InstrumentGraphCustomizationPropertiesEntity> findByFillColor(String fillColor);

    List<InstrumentGraphCustomizationPropertiesEntity> findByLineType(LineTypeEnum lineType);

    boolean existsByNameAndPatternId(String name, Long patternId);

    @Query("SELECT p FROM InstrumentGraphCustomizationPropertiesEntity p "
            + "WHERE p.pattern.instrument.id = :instrumentId")
    List<InstrumentGraphCustomizationPropertiesEntity> findByPatternInstrumentId(@Param("instrumentId") Long instrumentId);

    void deleteByPatternId(Long patternId);
}
