package com.geosegbar.unit.entities;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InputEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.ReadingEntity;
import com.geosegbar.entities.SectionEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentEntity")
class InstrumentEntityTest extends BaseUnitTest {

    private DamEntity dam;
    private InstrumentTypeEntity instrumentType;
    private SectionEntity section;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        dam = TestDataBuilder.dam().build();
        instrumentType = new InstrumentTypeEntity();
        instrumentType.setId(1L);
        instrumentType.setName("Piezômetro");
        section = new SectionEntity();
        section.setId(1L);
        section.setName("Seção 1");
        now = LocalDateTime.now();
    }

    @Test
    @DisplayName("Should create instrument with all required fields")
    void shouldCreateInstrumentWithAllRequiredFields() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("Piezômetro 01");
        instrument.setLatitude(-23.5505);
        instrument.setLongitude(-46.6333);
        instrument.setNoLimit(false);
        instrument.setActive(true);
        instrument.setDam(dam);
        instrument.setInstrumentType(instrumentType);
        instrument.setLastUpdateVariablesDate(now);
        instrument.setIsLinimetricRuler(false);

        // Then
        assertThat(instrument).satisfies(i -> {
            assertThat(i.getId()).isEqualTo(1L);
            assertThat(i.getName()).isEqualTo("Piezômetro 01");
            assertThat(i.getLatitude()).isEqualTo(-23.5505);
            assertThat(i.getLongitude()).isEqualTo(-46.6333);
            assertThat(i.getNoLimit()).isFalse();
            assertThat(i.getActive()).isTrue();
            assertThat(i.getDam()).isEqualTo(dam);
            assertThat(i.getInstrumentType()).isEqualTo(instrumentType);
            assertThat(i.getLastUpdateVariablesDate()).isEqualTo(now);
            assertThat(i.getIsLinimetricRuler()).isFalse();
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        InstrumentEntity instrument = new InstrumentEntity(
                1L,
                "Piezômetro 01",
                "Crista da Barragem",
                100.5,
                -23.5505,
                -46.6333,
                false,
                dam,
                instrumentType,
                true,
                true,
                now,
                false,
                null,
                section,
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>()
        );

        // Then
        assertThat(instrument.getId()).isEqualTo(1L);
        assertThat(instrument.getName()).isEqualTo("Piezômetro 01");
        assertThat(instrument.getLocation()).isEqualTo("Crista da Barragem");
        assertThat(instrument.getDistanceOffset()).isEqualTo(100.5);
        assertThat(instrument.getLatitude()).isEqualTo(-23.5505);
        assertThat(instrument.getLongitude()).isEqualTo(-46.6333);
        assertThat(instrument.getNoLimit()).isFalse();
        assertThat(instrument.getDam()).isEqualTo(dam);
        assertThat(instrument.getInstrumentType()).isEqualTo(instrumentType);
        assertThat(instrument.getActive()).isTrue();
        assertThat(instrument.getActiveForSection()).isTrue();
        assertThat(instrument.getLastUpdateVariablesDate()).isEqualTo(now);
        assertThat(instrument.getIsLinimetricRuler()).isFalse();
        assertThat(instrument.getSection()).isEqualTo(section);
    }

    @Test
    @DisplayName("Should default activeForSection to true")
    void shouldDefaultActiveForSectionToTrue() {
        // Given & When
        InstrumentEntity instrument = new InstrumentEntity();

        // Then
        assertThat(instrument.getActiveForSection()).isTrue();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Dam")
    void shouldMaintainManyToOneRelationshipWithDam() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setDam(dam);

        // Then
        assertThat(instrument.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with InstrumentType")
    void shouldMaintainManyToOneRelationshipWithInstrumentType() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setInstrumentType(instrumentType);

        // Then
        assertThat(instrument.getInstrumentType())
                .isNotNull()
                .isEqualTo(instrumentType);
    }

    @Test
    @DisplayName("Should maintain optional ManyToOne relationship with Section")
    void shouldMaintainOptionalManyToOneRelationshipWithSection() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setSection(section);

        // Then
        assertThat(instrument.getSection())
                .isNotNull()
                .isEqualTo(section);
    }

    @Test
    @DisplayName("Should allow instrument without section")
    void shouldAllowInstrumentWithoutSection() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setSection(null);

        // Then
        assertThat(instrument.getSection()).isNull();
    }

    @Test
    @DisplayName("Should validate positive latitude")
    void shouldValidatePositiveLatitude() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setLatitude(45.5);

        // Then
        assertThat(instrument.getLatitude()).isPositive();
    }

    @Test
    @DisplayName("Should validate negative latitude")
    void shouldValidateNegativeLatitude() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setLatitude(-23.5505);

        // Then
        assertThat(instrument.getLatitude()).isNegative();
    }

    @Test
    @DisplayName("Should validate positive longitude")
    void shouldValidatePositiveLongitude() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setLongitude(120.5);

        // Then
        assertThat(instrument.getLongitude()).isPositive();
    }

    @Test
    @DisplayName("Should validate negative longitude")
    void shouldValidateNegativeLongitude() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setLongitude(-46.6333);

        // Then
        assertThat(instrument.getLongitude()).isNegative();
    }

    @Test
    @DisplayName("Should support noLimit flag")
    void shouldSupportNoLimitFlag() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setNoLimit(true);

        // Then
        assertThat(instrument.getNoLimit()).isTrue();
    }

    @Test
    @DisplayName("Should support active flag")
    void shouldSupportActiveFlag() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setActive(true);

        // When
        instrument.setActive(false);

        // Then
        assertThat(instrument.getActive()).isFalse();
    }

    @Test
    @DisplayName("Should support activeForSection flag")
    void shouldSupportActiveForSectionFlag() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setActiveForSection(true);

        // When
        instrument.setActiveForSection(false);

        // Then
        assertThat(instrument.getActiveForSection()).isFalse();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of inputs")
    void shouldMaintainOneToManyCollectionOfInputs() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setInputs(new HashSet<>());

        InputEntity input = new InputEntity();
        input.setId(1L);
        input.setAcronym("IN01");
        instrument.getInputs().add(input);

        // Then
        assertThat(instrument.getInputs())
                .isNotNull()
                .hasSize(1)
                .contains(input);
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of constants")
    void shouldMaintainOneToManyCollectionOfConstants() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setConstants(new HashSet<>());

        ConstantEntity constant = new ConstantEntity();
        constant.setId(1L);
        constant.setAcronym("π");
        instrument.getConstants().add(constant);

        // Then
        assertThat(instrument.getConstants())
                .isNotNull()
                .hasSize(1)
                .contains(constant);
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of outputs")
    void shouldMaintainOneToManyCollectionOfOutputs() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setOutputs(new HashSet<>());

        OutputEntity output = new OutputEntity();
        output.setId(1L);
        instrument.getOutputs().add(output);

        // Then
        assertThat(instrument.getOutputs())
                .isNotNull()
                .hasSize(1)
                .contains(output);
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of readings")
    void shouldMaintainOneToManyCollectionOfReadings() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setReadings(new HashSet<>());

        ReadingEntity reading = new ReadingEntity();
        reading.setId(1L);
        instrument.getReadings().add(reading);

        // Then
        assertThat(instrument.getReadings())
                .isNotNull()
                .hasSize(1)
                .contains(reading);
    }

    @Test
    @DisplayName("Should support multiple inputs per instrument")
    void shouldSupportMultipleInputsPerInstrument() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setInputs(new HashSet<>());

        InputEntity input1 = new InputEntity();
        input1.setId(1L);
        InputEntity input2 = new InputEntity();
        input2.setId(2L);

        instrument.getInputs().add(input1);
        instrument.getInputs().add(input2);

        // Then
        assertThat(instrument.getInputs()).hasSize(2);
    }

    @Test
    @DisplayName("Should support linimetric ruler configuration")
    void shouldSupportLinimetricRulerConfiguration() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setIsLinimetricRuler(true);
        instrument.setLinimetricRulerCode(12345L);

        // Then
        assertThat(instrument.getIsLinimetricRuler()).isTrue();
        assertThat(instrument.getLinimetricRulerCode()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("Should allow linimetric ruler without code")
    void shouldAllowLinimetricRulerWithoutCode() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setIsLinimetricRuler(true);
        instrument.setLinimetricRulerCode(null);

        // Then
        assertThat(instrument.getIsLinimetricRuler()).isTrue();
        assertThat(instrument.getLinimetricRulerCode()).isNull();
    }

    @Test
    @DisplayName("Should track last update variables date")
    void shouldTrackLastUpdateVariablesDate() {
        // Given
        LocalDateTime updateDate = LocalDateTime.of(2024, 1, 15, 10, 30);
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setLastUpdateVariablesDate(updateDate);

        // Then
        assertThat(instrument.getLastUpdateVariablesDate()).isEqualTo(updateDate);
    }

    @Test
    @DisplayName("Should support optional location field")
    void shouldSupportOptionalLocationField() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setLocation("Crista da Barragem - Margem Direita");

        // Then
        assertThat(instrument.getLocation()).isEqualTo("Crista da Barragem - Margem Direita");
    }

    @Test
    @DisplayName("Should support optional distanceOffset field")
    void shouldSupportOptionalDistanceOffsetField() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setDistanceOffset(150.75);

        // Then
        assertThat(instrument.getDistanceOffset()).isEqualTo(150.75);
    }

    @Test
    @DisplayName("Should allow multiple instruments per dam")
    void shouldAllowMultipleInstrumentsPerDam() {
        // Given
        InstrumentEntity instrument1 = new InstrumentEntity();
        instrument1.setId(1L);
        instrument1.setDam(dam);

        InstrumentEntity instrument2 = new InstrumentEntity();
        instrument2.setId(2L);
        instrument2.setDam(dam);

        // Then
        assertThat(instrument1.getDam()).isEqualTo(instrument2.getDam());
        assertThat(instrument1.getId()).isNotEqualTo(instrument2.getId());
    }

    @Test
    @DisplayName("Should allow multiple instruments per section")
    void shouldAllowMultipleInstrumentsPerSection() {
        // Given
        InstrumentEntity instrument1 = new InstrumentEntity();
        instrument1.setId(1L);
        instrument1.setSection(section);

        InstrumentEntity instrument2 = new InstrumentEntity();
        instrument2.setId(2L);
        instrument2.setSection(section);

        // Then
        assertThat(instrument1.getSection()).isEqualTo(instrument2.getSection());
        assertThat(instrument1.getId()).isNotEqualTo(instrument2.getId());
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setActive(true);

        Long originalId = instrument.getId();

        // When
        instrument.setActive(false);
        instrument.setName("Novo Nome");

        // Then
        assertThat(instrument.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support deactivating instrument")
    void shouldSupportDeactivatingInstrument() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setActive(true);
        instrument.setActiveForSection(true);

        // When
        instrument.setActive(false);

        // Then
        assertThat(instrument.getActive()).isFalse();
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setName("Piezômetro de Nível d'Água");

        // Then
        assertThat(instrument.getName()).contains("ô", "'");
    }

    @Test
    @DisplayName("Should initialize empty collections by default")
    void shouldInitializeEmptyCollectionsByDefault() {
        // Given & When
        InstrumentEntity instrument = new InstrumentEntity();

        // Then
        assertThat(instrument.getInputs()).isNotNull().isEmpty();
        assertThat(instrument.getConstants()).isNotNull().isEmpty();
        assertThat(instrument.getOutputs()).isNotNull().isEmpty();
        assertThat(instrument.getReadings()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing inputs")
    void shouldSupportAddingAndRemovingInputs() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        InputEntity input = new InputEntity();
        input.setId(1L);

        // When
        instrument.getInputs().add(input);
        assertThat(instrument.getInputs()).hasSize(1);

        instrument.getInputs().remove(input);

        // Then
        assertThat(instrument.getInputs()).isEmpty();
    }

    @Test
    @DisplayName("Should support coordinate updates")
    void shouldSupportCoordinateUpdates() {
        // Given
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setLatitude(-23.5505);
        instrument.setLongitude(-46.6333);

        // When
        instrument.setLatitude(-22.9068);
        instrument.setLongitude(-43.1729);

        // Then
        assertThat(instrument.getLatitude()).isEqualTo(-22.9068);
        assertThat(instrument.getLongitude()).isEqualTo(-43.1729);
    }
}
