package com.geosegbar.unit.entities;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.LevelEntity;
import com.geosegbar.entities.ReservoirEntity;

@Tag("unit")
class ReservoirEntityTest extends BaseUnitTest {

    private DamEntity dam;
    private LevelEntity level;

    @BeforeEach
    void setUp() {
        dam = new DamEntity();
        dam.setId(1L);
        dam.setName("Barragem Principal");

        level = new LevelEntity();
        level.setId(1L);
        level.setName("Normal");
        level.setValue(100.0);
    }

    @Test
    @DisplayName("Should create reservoir with all required fields")
    void shouldCreateReservoirWithAllRequiredFields() {

        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setId(1L);
        reservoir.setDam(dam);
        reservoir.setLevel(level);

        assertThat(reservoir).satisfies(r -> {
            assertThat(r.getId()).isEqualTo(1L);
            assertThat(r.getDam()).isEqualTo(dam);
            assertThat(r.getLevel()).isEqualTo(level);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        LocalDateTime now = LocalDateTime.now();

        ReservoirEntity reservoir = new ReservoirEntity(
                1L,
                dam,
                level,
                now
        );

        assertThat(reservoir.getId()).isEqualTo(1L);
        assertThat(reservoir.getDam()).isEqualTo(dam);
        assertThat(reservoir.getLevel()).isEqualTo(level);
        assertThat(reservoir.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Dam")
    void shouldMaintainManyToOneRelationshipWithDam() {

        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setDam(dam);

        assertThat(reservoir.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Level")
    void shouldMaintainManyToOneRelationshipWithLevel() {

        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setLevel(level);

        assertThat(reservoir.getLevel())
                .isNotNull()
                .isEqualTo(level);
    }

    @Test
    @DisplayName("Should track createdAt timestamp")
    void shouldTrackCreatedAtTimestamp() {

        LocalDateTime specificTime = LocalDateTime.of(2024, 12, 28, 15, 30, 45);
        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setCreatedAt(specificTime);

        assertThat(reservoir.getCreatedAt()).isEqualTo(specificTime);
    }

    @Test
    @DisplayName("Should support multiple reservoirs per dam")
    void shouldSupportMultipleReservoirsPerDam() {

        LevelEntity level2 = new LevelEntity();
        level2.setId(2L);
        level2.setName("Atenção");

        ReservoirEntity reservoir1 = new ReservoirEntity();
        reservoir1.setId(1L);
        reservoir1.setDam(dam);
        reservoir1.setLevel(level);

        ReservoirEntity reservoir2 = new ReservoirEntity();
        reservoir2.setId(2L);
        reservoir2.setDam(dam);
        reservoir2.setLevel(level2);

        assertThat(reservoir1.getDam()).isEqualTo(reservoir2.getDam());
        assertThat(reservoir1.getLevel()).isNotEqualTo(reservoir2.getLevel());
    }

    @Test
    @DisplayName("Should support multiple reservoirs per level")
    void shouldSupportMultipleReservoirsPerLevel() {

        DamEntity dam2 = new DamEntity();
        dam2.setId(2L);
        dam2.setName("Barragem Secundária");

        ReservoirEntity reservoir1 = new ReservoirEntity();
        reservoir1.setId(1L);
        reservoir1.setDam(dam);
        reservoir1.setLevel(level);

        ReservoirEntity reservoir2 = new ReservoirEntity();
        reservoir2.setId(2L);
        reservoir2.setDam(dam2);
        reservoir2.setLevel(level);

        assertThat(reservoir1.getLevel()).isEqualTo(reservoir2.getLevel());
        assertThat(reservoir1.getDam()).isNotEqualTo(reservoir2.getDam());
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setId(1L);
        reservoir.setDam(dam);
        reservoir.setLevel(level);

        Long originalId = reservoir.getId();

        LevelEntity newLevel = new LevelEntity();
        newLevel.setId(3L);
        newLevel.setName("Alerta");
        reservoir.setLevel(newLevel);

        assertThat(reservoir.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support different levels for same dam")
    void shouldSupportDifferentLevelsForSameDam() {

        LevelEntity normalLevel = new LevelEntity();
        normalLevel.setId(1L);
        normalLevel.setName("Normal");
        normalLevel.setValue(100.0);

        LevelEntity alertLevel = new LevelEntity();
        alertLevel.setId(2L);
        alertLevel.setName("Alerta");
        alertLevel.setValue(120.0);

        LevelEntity emergencyLevel = new LevelEntity();
        emergencyLevel.setId(3L);
        emergencyLevel.setName("Emergência");
        emergencyLevel.setValue(150.0);

        ReservoirEntity reservoir1 = new ReservoirEntity();
        reservoir1.setDam(dam);
        reservoir1.setLevel(normalLevel);

        ReservoirEntity reservoir2 = new ReservoirEntity();
        reservoir2.setDam(dam);
        reservoir2.setLevel(alertLevel);

        ReservoirEntity reservoir3 = new ReservoirEntity();
        reservoir3.setDam(dam);
        reservoir3.setLevel(emergencyLevel);

        assertThat(reservoir1.getDam()).isEqualTo(dam);
        assertThat(reservoir2.getDam()).isEqualTo(dam);
        assertThat(reservoir3.getDam()).isEqualTo(dam);
        assertThat(reservoir1.getLevel().getValue()).isLessThan(reservoir2.getLevel().getValue());
        assertThat(reservoir2.getLevel().getValue()).isLessThan(reservoir3.getLevel().getValue());
    }

    @Test
    @DisplayName("Should support createdAt for audit trail")
    void shouldSupportCreatedAtForAuditTrail() {

        LocalDateTime time1 = LocalDateTime.of(2024, 12, 1, 10, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 12, 15, 14, 30);

        ReservoirEntity reservoir1 = new ReservoirEntity();
        reservoir1.setCreatedAt(time1);

        ReservoirEntity reservoir2 = new ReservoirEntity();
        reservoir2.setCreatedAt(time2);

        assertThat(reservoir1.getCreatedAt()).isBefore(reservoir2.getCreatedAt());
    }

    @Test
    @DisplayName("Should support historical reservoir records")
    void shouldSupportHistoricalReservoirRecords() {

        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setId(1L);
        reservoir.setDam(dam);
        reservoir.setLevel(level);
        reservoir.setCreatedAt(LocalDateTime.now());

        assertThat(reservoir.getCreatedAt()).isNotNull();
        assertThat(reservoir.getDam()).isNotNull();
        assertThat(reservoir.getLevel()).isNotNull();
    }

    @Test
    @DisplayName("Should support reservoir with specific timestamp")
    void shouldSupportReservoirWithSpecificTimestamp() {

        LocalDateTime specificTime = LocalDateTime.of(2024, 12, 28, 8, 15, 30);
        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setDam(dam);
        reservoir.setLevel(level);
        reservoir.setCreatedAt(specificTime);

        assertThat(reservoir.getCreatedAt().getYear()).isEqualTo(2024);
        assertThat(reservoir.getCreatedAt().getMonthValue()).isEqualTo(12);
        assertThat(reservoir.getCreatedAt().getDayOfMonth()).isEqualTo(28);
        assertThat(reservoir.getCreatedAt().getHour()).isEqualTo(8);
        assertThat(reservoir.getCreatedAt().getMinute()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should support bidirectional relationship with Dam")
    void shouldSupportBidirectionalRelationshipWithDam() {

        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setDam(dam);

        assertThat(reservoir.getDam()).isEqualTo(dam);
    }

    @Test
    @DisplayName("Should support bidirectional relationship with Level")
    void shouldSupportBidirectionalRelationshipWithLevel() {

        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setLevel(level);

        assertThat(reservoir.getLevel()).isEqualTo(level);
    }

    @Test
    @DisplayName("Should support composite index query pattern")
    void shouldSupportCompositeIndexQueryPattern() {

        ReservoirEntity reservoir1 = new ReservoirEntity();
        reservoir1.setDam(dam);
        reservoir1.setLevel(level);

        LevelEntity level2 = new LevelEntity();
        level2.setId(2L);
        level2.setName("Atenção");

        ReservoirEntity reservoir2 = new ReservoirEntity();
        reservoir2.setDam(dam);
        reservoir2.setLevel(level2);

        assertThat(reservoir1.getDam().getId()).isEqualTo(1L);
        assertThat(reservoir1.getLevel().getId()).isEqualTo(1L);
        assertThat(reservoir2.getDam().getId()).isEqualTo(1L);
        assertThat(reservoir2.getLevel().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should support time-series data tracking")
    void shouldSupportTimeSeriesDataTracking() {

        LocalDateTime t1 = LocalDateTime.of(2024, 12, 28, 8, 0);
        LocalDateTime t2 = LocalDateTime.of(2024, 12, 28, 12, 0);
        LocalDateTime t3 = LocalDateTime.of(2024, 12, 28, 16, 0);

        LevelEntity normalLevel = new LevelEntity();
        normalLevel.setId(1L);
        normalLevel.setValue(100.0);

        LevelEntity attentionLevel = new LevelEntity();
        attentionLevel.setId(2L);
        attentionLevel.setValue(110.0);

        LevelEntity alertLevel = new LevelEntity();
        alertLevel.setId(3L);
        alertLevel.setValue(120.0);

        ReservoirEntity reading1 = new ReservoirEntity();
        reading1.setDam(dam);
        reading1.setLevel(normalLevel);
        reading1.setCreatedAt(t1);

        ReservoirEntity reading2 = new ReservoirEntity();
        reading2.setDam(dam);
        reading2.setLevel(attentionLevel);
        reading2.setCreatedAt(t2);

        ReservoirEntity reading3 = new ReservoirEntity();
        reading3.setDam(dam);
        reading3.setLevel(alertLevel);
        reading3.setCreatedAt(t3);

        assertThat(reading1.getLevel().getValue()).isLessThan(reading2.getLevel().getValue());
        assertThat(reading2.getLevel().getValue()).isLessThan(reading3.getLevel().getValue());
        assertThat(reading1.getCreatedAt()).isBefore(reading2.getCreatedAt());
        assertThat(reading2.getCreatedAt()).isBefore(reading3.getCreatedAt());
    }
}
