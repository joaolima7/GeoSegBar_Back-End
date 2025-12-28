package com.geosegbar.unit.entities;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DocumentationDamEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - DocumentationDamEntity")
class DocumentationDamEntityTest extends BaseUnitTest {

    private DamEntity dam;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        dam = TestDataBuilder.dam().build();
    }

    @Test
    @DisplayName("Should create documentation dam with all date fields")
    void shouldCreateDocumentationDamWithAllDateFields() {

        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(1);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setId(1L);
        documentation.setDam(dam);
        documentation.setLastUpdatePAE(today);
        documentation.setNextUpdatePAE(nextMonth);
        documentation.setLastUpdatePSB(today);
        documentation.setNextUpdatePSB(nextMonth);
        documentation.setLastUpdateRPSB(today);
        documentation.setNextUpdateRPSB(nextMonth);
        documentation.setLastAchievementISR(today);
        documentation.setNextAchievementISR(nextMonth);
        documentation.setLastAchievementChecklist(today);
        documentation.setNextAchievementChecklist(nextMonth);
        documentation.setLastFillingFSB(today);
        documentation.setNextFillingFSB(nextMonth);
        documentation.setLastInternalSimulation(today);
        documentation.setNextInternalSimulation(nextMonth);
        documentation.setLastExternalSimulation(today);
        documentation.setNextExternalSimulation(nextMonth);

        assertThat(documentation).satisfies(d -> {
            assertThat(d.getId()).isEqualTo(1L);
            assertThat(d.getDam()).isEqualTo(dam);
            assertThat(d.getLastUpdatePAE()).isEqualTo(today);
            assertThat(d.getNextUpdatePAE()).isEqualTo(nextMonth);
            assertThat(d.getLastUpdatePSB()).isEqualTo(today);
            assertThat(d.getNextUpdatePSB()).isEqualTo(nextMonth);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusWeeks(1);

        DocumentationDamEntity documentation = new DocumentationDamEntity(
                1L,
                dam,
                today, nextWeek,
                today, nextWeek,
                today, nextWeek,
                today, nextWeek,
                today, nextWeek,
                today, nextWeek,
                today, nextWeek,
                today, nextWeek
        );

        assertThat(documentation.getId()).isEqualTo(1L);
        assertThat(documentation.getDam()).isEqualTo(dam);
        assertThat(documentation.getLastUpdatePAE()).isEqualTo(today);
        assertThat(documentation.getNextUpdatePAE()).isEqualTo(nextWeek);
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with Dam")
    void shouldMaintainOneToOneRelationshipWithDam() {

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setDam(dam);

        assertThat(documentation.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should track PAE dates independently")
    void shouldTrackPaeDatesIndependently() {

        LocalDate lastUpdate = LocalDate.of(2024, 1, 1);
        LocalDate nextUpdate = LocalDate.of(2025, 1, 1);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastUpdatePAE(lastUpdate);
        documentation.setNextUpdatePAE(nextUpdate);

        assertThat(documentation.getLastUpdatePAE()).isEqualTo(lastUpdate);
        assertThat(documentation.getNextUpdatePAE()).isEqualTo(nextUpdate);
        assertThat(documentation.getNextUpdatePAE()).isAfter(documentation.getLastUpdatePAE());
    }

    @Test
    @DisplayName("Should track PSB dates independently")
    void shouldTrackPsbDatesIndependently() {

        LocalDate lastUpdate = LocalDate.of(2024, 6, 1);
        LocalDate nextUpdate = LocalDate.of(2025, 6, 1);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastUpdatePSB(lastUpdate);
        documentation.setNextUpdatePSB(nextUpdate);

        assertThat(documentation.getLastUpdatePSB()).isEqualTo(lastUpdate);
        assertThat(documentation.getNextUpdatePSB()).isEqualTo(nextUpdate);
    }

    @Test
    @DisplayName("Should track RPSB dates independently")
    void shouldTrackRpsbDatesIndependently() {

        LocalDate lastUpdate = LocalDate.of(2024, 3, 15);
        LocalDate nextUpdate = LocalDate.of(2025, 3, 15);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastUpdateRPSB(lastUpdate);
        documentation.setNextUpdateRPSB(nextUpdate);

        assertThat(documentation.getLastUpdateRPSB()).isEqualTo(lastUpdate);
        assertThat(documentation.getNextUpdateRPSB()).isEqualTo(nextUpdate);
    }

    @Test
    @DisplayName("Should track ISR achievement dates")
    void shouldTrackIsrAchievementDates() {

        LocalDate lastAchievement = LocalDate.of(2024, 2, 1);
        LocalDate nextAchievement = LocalDate.of(2024, 8, 1);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastAchievementISR(lastAchievement);
        documentation.setNextAchievementISR(nextAchievement);

        assertThat(documentation.getLastAchievementISR()).isEqualTo(lastAchievement);
        assertThat(documentation.getNextAchievementISR()).isEqualTo(nextAchievement);
    }

    @Test
    @DisplayName("Should track checklist achievement dates")
    void shouldTrackChecklistAchievementDates() {

        LocalDate lastAchievement = LocalDate.now().minusDays(7);
        LocalDate nextAchievement = LocalDate.now().plusDays(23);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastAchievementChecklist(lastAchievement);
        documentation.setNextAchievementChecklist(nextAchievement);

        assertThat(documentation.getLastAchievementChecklist()).isEqualTo(lastAchievement);
        assertThat(documentation.getNextAchievementChecklist()).isEqualTo(nextAchievement);
    }

    @Test
    @DisplayName("Should track FSB filling dates")
    void shouldTrackFsbFillingDates() {

        LocalDate lastFilling = LocalDate.of(2024, 1, 10);
        LocalDate nextFilling = LocalDate.of(2024, 7, 10);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastFillingFSB(lastFilling);
        documentation.setNextFillingFSB(nextFilling);

        assertThat(documentation.getLastFillingFSB()).isEqualTo(lastFilling);
        assertThat(documentation.getNextFillingFSB()).isEqualTo(nextFilling);
    }

    @Test
    @DisplayName("Should track internal simulation dates")
    void shouldTrackInternalSimulationDates() {

        LocalDate lastSimulation = LocalDate.of(2024, 4, 1);
        LocalDate nextSimulation = LocalDate.of(2025, 4, 1);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastInternalSimulation(lastSimulation);
        documentation.setNextInternalSimulation(nextSimulation);

        assertThat(documentation.getLastInternalSimulation()).isEqualTo(lastSimulation);
        assertThat(documentation.getNextInternalSimulation()).isEqualTo(nextSimulation);
    }

    @Test
    @DisplayName("Should track external simulation dates")
    void shouldTrackExternalSimulationDates() {

        LocalDate lastSimulation = LocalDate.of(2024, 5, 1);
        LocalDate nextSimulation = LocalDate.of(2025, 5, 1);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastExternalSimulation(lastSimulation);
        documentation.setNextExternalSimulation(nextSimulation);

        assertThat(documentation.getLastExternalSimulation()).isEqualTo(lastSimulation);
        assertThat(documentation.getNextExternalSimulation()).isEqualTo(nextSimulation);
    }

    @Test
    @DisplayName("Should allow all date fields to be null")
    void shouldAllowAllDateFieldsToBeNull() {

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setDam(dam);

        assertThat(documentation.getLastUpdatePAE()).isNull();
        assertThat(documentation.getNextUpdatePAE()).isNull();
        assertThat(documentation.getLastUpdatePSB()).isNull();
        assertThat(documentation.getNextUpdatePSB()).isNull();
        assertThat(documentation.getLastUpdateRPSB()).isNull();
        assertThat(documentation.getNextUpdateRPSB()).isNull();
    }

    @Test
    @DisplayName("Should handle partial date tracking")
    void shouldHandlePartialDateTracking() {

        LocalDate today = LocalDate.now();
        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastUpdatePAE(today);

        assertThat(documentation.getLastUpdatePAE()).isNotNull();
        assertThat(documentation.getNextUpdatePAE()).isNull();
    }

    @Test
    @DisplayName("Should calculate different intervals for different document types")
    void shouldCalculateDifferentIntervalsForDifferentDocumentTypes() {

        LocalDate baseDate = LocalDate.of(2024, 1, 1);
        DocumentationDamEntity documentation = new DocumentationDamEntity();

        documentation.setLastUpdatePAE(baseDate);
        documentation.setNextUpdatePAE(baseDate.plusYears(1));

        documentation.setLastAchievementChecklist(baseDate);
        documentation.setNextAchievementChecklist(baseDate.plusMonths(1));

        assertThat(documentation.getNextUpdatePAE()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(documentation.getNextAchievementChecklist()).isEqualTo(LocalDate.of(2024, 2, 1));
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setId(1L);
        documentation.setLastUpdatePAE(LocalDate.of(2024, 1, 1));

        Long originalId = documentation.getId();

        documentation.setLastUpdatePAE(LocalDate.of(2024, 6, 1));
        documentation.setNextUpdatePAE(LocalDate.of(2025, 6, 1));

        assertThat(documentation.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should handle overdue next dates")
    void shouldHandleOverdueNextDates() {

        LocalDate pastDate = LocalDate.now().minusMonths(6);
        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setNextUpdatePAE(pastDate);

        assertThat(documentation.getNextUpdatePAE()).isBefore(LocalDate.now());
    }

    @Test
    @DisplayName("Should support different date formats for different regions")
    void shouldSupportDifferentDateFormatsForDifferentRegions() {

        LocalDate brazilianDate = LocalDate.of(2024, 12, 25);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastUpdatePSB(brazilianDate);

        assertThat(documentation.getLastUpdatePSB()).isEqualTo(brazilianDate);
        assertThat(documentation.getLastUpdatePSB().getDayOfMonth()).isEqualTo(25);
        assertThat(documentation.getLastUpdatePSB().getMonthValue()).isEqualTo(12);
    }

    @Test
    @DisplayName("Should handle leap year dates")
    void shouldHandleLeapYearDates() {

        LocalDate leapYearDate = LocalDate.of(2024, 2, 29);

        DocumentationDamEntity documentation = new DocumentationDamEntity();
        documentation.setLastUpdateRPSB(leapYearDate);

        assertThat(documentation.getLastUpdateRPSB()).isEqualTo(leapYearDate);
    }
}
