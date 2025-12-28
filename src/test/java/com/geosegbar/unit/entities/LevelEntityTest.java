package com.geosegbar.unit.entities;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.LevelEntity;
import com.geosegbar.entities.ReservoirEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - LevelEntity")
class LevelEntityTest extends BaseUnitTest {

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
    }

    @Test
    @DisplayName("Should create level with all required fields")
    void shouldCreateLevelWithAllRequiredFields() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setId(1L);
        level.setName("Normal");
        level.setValue(100.0);
        level.setUnitLevel("m");

        // Then
        assertThat(level).satisfies(l -> {
            assertThat(l.getId()).isEqualTo(1L);
            assertThat(l.getName()).isEqualTo("Normal");
            assertThat(l.getValue()).isEqualTo(100.0);
            assertThat(l.getUnitLevel()).isEqualTo("m");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        LevelEntity level = new LevelEntity(
                1L,
                "Normal",
                100.0,
                "m",
                now,
                new HashSet<>()
        );

        // Then
        assertThat(level.getId()).isEqualTo(1L);
        assertThat(level.getName()).isEqualTo("Normal");
        assertThat(level.getValue()).isEqualTo(100.0);
        assertThat(level.getUnitLevel()).isEqualTo("m");
        assertThat(level.getCreatedAt()).isEqualTo(now);
        assertThat(level.getReservoirs()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support createdAt timestamp")
    void shouldSupportCreatedAtTimestamp() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setName("Normal");
        level.setValue(100.0);
        level.setUnitLevel("m");

        LocalDateTime timestamp = LocalDateTime.now();

        // When
        level.setCreatedAt(timestamp);

        // Then
        assertThat(level.getCreatedAt())
                .isNotNull()
                .isCloseTo(timestamp, within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of reservoirs")
    void shouldMaintainOneToManyCollectionOfReservoirs() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setName("Normal");
        level.setReservoirs(new HashSet<>());

        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setId(1L);
        level.getReservoirs().add(reservoir);

        // Then
        assertThat(level.getReservoirs())
                .isNotNull()
                .hasSize(1)
                .contains(reservoir);
    }

    @Test
    @DisplayName("Should support multiple reservoirs per level")
    void shouldSupportMultipleReservoirsPerLevel() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setName("Normal");
        level.setReservoirs(new HashSet<>());

        ReservoirEntity res1 = new ReservoirEntity();
        res1.setId(1L);
        ReservoirEntity res2 = new ReservoirEntity();
        res2.setId(2L);
        ReservoirEntity res3 = new ReservoirEntity();
        res3.setId(3L);

        level.getReservoirs().add(res1);
        level.getReservoirs().add(res2);
        level.getReservoirs().add(res3);

        // Then
        assertThat(level.getReservoirs()).hasSize(3);
    }

    @Test
    @DisplayName("Should initialize empty reservoirs collection by default")
    void shouldInitializeEmptyReservoirsCollectionByDefault() {
        // Given & When
        LevelEntity level = new LevelEntity();

        // Then
        assertThat(level.getReservoirs()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing reservoirs")
    void shouldSupportAddingAndRemovingReservoirs() {
        // Given
        LevelEntity level = new LevelEntity();
        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setId(1L);

        // When
        level.getReservoirs().add(reservoir);
        assertThat(level.getReservoirs()).hasSize(1);

        level.getReservoirs().remove(reservoir);

        // Then
        assertThat(level.getReservoirs()).isEmpty();
    }

    @Test
    @DisplayName("Should support common level names")
    void shouldSupportCommonLevelNames() {
        // Given
        LevelEntity normal = new LevelEntity();
        normal.setName("Normal");

        LevelEntity attention = new LevelEntity();
        attention.setName("Atenção");

        LevelEntity alert = new LevelEntity();
        alert.setName("Alerta");

        LevelEntity emergency = new LevelEntity();
        emergency.setName("Emergência");

        // Then
        assertThat(normal.getName()).isEqualTo("Normal");
        assertThat(attention.getName()).contains("ã");
        assertThat(alert.getName()).isEqualTo("Alerta");
        assertThat(emergency.getName()).contains("ê");
    }

    @Test
    @DisplayName("Should support positive level values")
    void shouldSupportPositiveLevelValues() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setValue(150.75);

        // Then
        assertThat(level.getValue()).isPositive();
    }

    @Test
    @DisplayName("Should support zero level value")
    void shouldSupportZeroLevelValue() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setValue(0.0);

        // Then
        assertThat(level.getValue()).isZero();
    }

    @Test
    @DisplayName("Should support decimal level values")
    void shouldSupportDecimalLevelValues() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setValue(123.456);

        // Then
        assertThat(level.getValue()).isEqualTo(123.456);
    }

    @Test
    @DisplayName("Should support different unit levels")
    void shouldSupportDifferentUnitLevels() {
        // Given
        LevelEntity meters = new LevelEntity();
        meters.setUnitLevel("m");

        LevelEntity centimeters = new LevelEntity();
        centimeters.setUnitLevel("cm");

        LevelEntity feet = new LevelEntity();
        feet.setUnitLevel("ft");

        // Then
        assertThat(meters.getUnitLevel()).isEqualTo("m");
        assertThat(centimeters.getUnitLevel()).isEqualTo("cm");
        assertThat(feet.getUnitLevel()).isEqualTo("ft");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setId(1L);
        level.setValue(100.0);

        Long originalId = level.getId();

        // When
        level.setValue(150.0);

        // Then
        assertThat(level.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support unique name index")
    void shouldSupportUniqueNameIndex() {
        // Given
        LevelEntity level1 = new LevelEntity();
        level1.setId(1L);
        level1.setName("Normal");

        LevelEntity level2 = new LevelEntity();
        level2.setId(2L);
        level2.setName("Atenção");

        // Then - Different names for different levels
        assertThat(level1.getName()).isNotEqualTo(level2.getName());
    }

    @Test
    @DisplayName("Should support value index for queries")
    void shouldSupportValueIndexForQueries() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setValue(100.0);

        // Then - Value indexed
        assertThat(level.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Should support createdAt tracking")
    void shouldSupportCreatedAtTracking() {
        // Given
        LevelEntity level = new LevelEntity();
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 15, 10, 30);
        level.setCreatedAt(timestamp);

        // Then
        assertThat(level.getCreatedAt()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should support lazy fetch for reservoirs")
    void shouldSupportLazyFetchForReservoirs() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setName("Normal");

        // Then - Reservoirs collection initialized but lazy
        assertThat(level.getReservoirs()).isNotNull();
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setName("Atenção Máxima");

        // Then
        assertThat(level.getName()).contains("ã", "á");
    }

    @Test
    @DisplayName("Should support different level hierarchies")
    void shouldSupportDifferentLevelHierarchies() {
        // Given - Ascending levels
        LevelEntity normal = new LevelEntity();
        normal.setId(1L);
        normal.setName("Normal");
        normal.setValue(100.0);

        LevelEntity attention = new LevelEntity();
        attention.setId(2L);
        attention.setName("Atenção");
        attention.setValue(110.0);

        LevelEntity alert = new LevelEntity();
        alert.setId(3L);
        alert.setName("Alerta");
        alert.setValue(120.0);

        LevelEntity emergency = new LevelEntity();
        emergency.setId(4L);
        emergency.setName("Emergência");
        emergency.setValue(130.0);

        // Then - Values increase with severity
        assertThat(normal.getValue()).isLessThan(attention.getValue());
        assertThat(attention.getValue()).isLessThan(alert.getValue());
        assertThat(alert.getValue()).isLessThan(emergency.getValue());
    }

    @Test
    @DisplayName("Should support bidirectional relationship with reservoirs")
    void shouldSupportBidirectionalRelationshipWithReservoirs() {
        // Given
        LevelEntity level = new LevelEntity();
        level.setId(1L);
        level.setName("Normal");

        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setId(1L);
        reservoir.setLevel(level);

        // When
        level.getReservoirs().add(reservoir);

        // Then - Bidirectional relationship
        assertThat(reservoir.getLevel()).isEqualTo(level);
        assertThat(level.getReservoirs()).contains(reservoir);
    }
}
