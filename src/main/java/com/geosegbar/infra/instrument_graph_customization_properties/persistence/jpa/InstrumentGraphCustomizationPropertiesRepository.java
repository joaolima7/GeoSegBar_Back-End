package com.geosegbar.infra.instrument_graph_customization_properties.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LimitValueTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;
import com.geosegbar.entities.InstrumentGraphCustomizationPropertiesEntity;

import jakarta.persistence.QueryHint;

@Repository
public interface InstrumentGraphCustomizationPropertiesRepository extends JpaRepository<InstrumentGraphCustomizationPropertiesEntity, Long> {

    @EntityGraph(attributePaths = {
        "pattern",
        "output", "output.measurementUnit",
        "statisticalLimit", "statisticalLimit.output", "statisticalLimit.output.measurementUnit",
        "deterministicLimit", "deterministicLimit.output", "deterministicLimit.output.measurementUnit",
        "instrument",
        "constant", "constant.measurementUnit"
    })
    @QueryHints(
            @QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<InstrumentGraphCustomizationPropertiesEntity> findByPatternId(Long patternId);

    @EntityGraph(attributePaths = {"output", "constant", "statisticalLimit", "deterministicLimit", "instrument"})
    List<InstrumentGraphCustomizationPropertiesEntity> findByConstantId(Long constantId);

    @EntityGraph(attributePaths = {"output", "constant", "statisticalLimit", "deterministicLimit", "instrument"})
    List<InstrumentGraphCustomizationPropertiesEntity> findByCustomizationType(CustomizationTypeEnum customizationType);

    @EntityGraph(attributePaths = {"pattern", "output", "constant", "statisticalLimit", "deterministicLimit", "instrument"})
    List<InstrumentGraphCustomizationPropertiesEntity> findByPatternIdAndCustomizationType(
            Long patternId, CustomizationTypeEnum customizationType);

    @EntityGraph(attributePaths = {"pattern"})
    Optional<InstrumentGraphCustomizationPropertiesEntity> findByNameAndPatternId(String name, Long patternId);

    @EntityGraph(attributePaths = {"pattern", "output", "statisticalLimit", "deterministicLimit"})
    List<InstrumentGraphCustomizationPropertiesEntity> findByOutputId(Long outputId);

    @EntityGraph(attributePaths = {"pattern", "statisticalLimit"})
    List<InstrumentGraphCustomizationPropertiesEntity> findByStatisticalLimitId(Long statisticalLimitId);

    @EntityGraph(attributePaths = {"pattern", "deterministicLimit"})
    List<InstrumentGraphCustomizationPropertiesEntity> findByDeterministicLimitId(Long deterministicLimitId);

    @EntityGraph(attributePaths = {"pattern", "instrument"})
    List<InstrumentGraphCustomizationPropertiesEntity> findByInstrumentId(Long instrumentId);

    List<InstrumentGraphCustomizationPropertiesEntity> findByLabelEnableTrue();

    List<InstrumentGraphCustomizationPropertiesEntity> findByIsPrimaryOrdinateTrue();

    List<InstrumentGraphCustomizationPropertiesEntity> findByIsPrimaryOrdinateFalse();

    List<InstrumentGraphCustomizationPropertiesEntity> findByFillColor(String fillColor);

    List<InstrumentGraphCustomizationPropertiesEntity> findByLineType(LineTypeEnum lineType);

    boolean existsByNameAndPatternId(String name, Long patternId);

    @EntityGraph(attributePaths = {"pattern", "instrument", "output", "constant"})
    @Query("SELECT p FROM InstrumentGraphCustomizationPropertiesEntity p "
            + "WHERE p.pattern.instrument.id = :instrumentId")
    List<InstrumentGraphCustomizationPropertiesEntity> findByPatternInstrumentId(@Param("instrumentId") Long instrumentId);

    void deleteByPatternId(Long patternId);

    @EntityGraph(attributePaths = {"statisticalLimit", "pattern"})
    List<InstrumentGraphCustomizationPropertiesEntity> findByPatternIdAndStatisticalLimitIdAndLimitValueType(
            Long patternId, Long statisticalLimitId, LimitValueTypeEnum limitValueType);

    @EntityGraph(attributePaths = {"deterministicLimit", "pattern"})
    List<InstrumentGraphCustomizationPropertiesEntity> findByPatternIdAndDeterministicLimitIdAndLimitValueType(
            Long patternId, Long deterministicLimitId, LimitValueTypeEnum limitValueType);

    @EntityGraph(attributePaths = {"statisticalLimit", "pattern"})
    @Query("SELECT p FROM InstrumentGraphCustomizationPropertiesEntity p "
            + "WHERE p.pattern.id = :patternId AND p.customizationType = 'STATISTICAL_LIMIT' "
            + "AND p.statisticalLimit.id = :limitId")
    List<InstrumentGraphCustomizationPropertiesEntity> findStatisticalLimitPropertiesByPatternAndLimit(
            @Param("patternId") Long patternId, @Param("limitId") Long limitId);

    @EntityGraph(attributePaths = {"deterministicLimit", "pattern"})
    @Query("SELECT p FROM InstrumentGraphCustomizationPropertiesEntity p "
            + "WHERE p.pattern.id = :patternId AND p.customizationType = 'DETERMINISTIC_LIMIT' "
            + "AND p.deterministicLimit.id = :limitId")
    List<InstrumentGraphCustomizationPropertiesEntity> findDeterministicLimitPropertiesByPatternAndLimit(
            @Param("patternId") Long patternId, @Param("limitId") Long limitId);
}
