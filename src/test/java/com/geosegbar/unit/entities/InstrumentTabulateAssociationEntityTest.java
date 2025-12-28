package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTabulateAssociationEntity;
import com.geosegbar.entities.InstrumentTabulateOutputAssociationEntity;
import com.geosegbar.entities.InstrumentTabulatePatternEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentTabulateAssociationEntity")
class InstrumentTabulateAssociationEntityTest extends BaseUnitTest {

    private InstrumentTabulatePatternEntity pattern;
    private InstrumentEntity instrument;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        pattern = new InstrumentTabulatePatternEntity();
        pattern.setId(1L);
        pattern.setName("Pattern 1");

        instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("Instrumento 1");
    }

    @Test
    @DisplayName("Should create tabulate association with all required fields")
    void shouldCreateTabulateAssociationWithAllRequiredFields() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(1L);
        association.setPattern(pattern);
        association.setInstrument(instrument);

        assertThat(association).satisfies(a -> {
            assertThat(a.getId()).isEqualTo(1L);
            assertThat(a.getPattern()).isEqualTo(pattern);
            assertThat(a.getInstrument()).isEqualTo(instrument);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity(
                1L,
                pattern,
                instrument,
                true,
                0,
                true,
                1,
                true,
                2,
                true,
                new HashSet<>()
        );

        assertThat(association.getId()).isEqualTo(1L);
        assertThat(association.getPattern()).isEqualTo(pattern);
        assertThat(association.getInstrument()).isEqualTo(instrument);
        assertThat(association.getIsDateEnable()).isTrue();
        assertThat(association.getDateIndex()).isEqualTo(0);
        assertThat(association.getIsHourEnable()).isTrue();
        assertThat(association.getHourIndex()).isEqualTo(1);
        assertThat(association.getIsUserEnable()).isTrue();
        assertThat(association.getUserIndex()).isEqualTo(2);
        assertThat(association.getIsReadEnable()).isTrue();
        assertThat(association.getOutputAssociations()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with InstrumentTabulatePattern")
    void shouldMaintainManyToOneRelationshipWithInstrumentTabulatePattern() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setPattern(pattern);

        assertThat(association.getPattern())
                .isNotNull()
                .isEqualTo(pattern);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Instrument")
    void shouldMaintainManyToOneRelationshipWithInstrument() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setInstrument(instrument);

        assertThat(association.getInstrument())
                .isNotNull()
                .isEqualTo(instrument);
    }

    @Test
    @DisplayName("Should enable date column")
    void shouldEnableDateColumn() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsDateEnable(true);
        association.setDateIndex(0);

        assertThat(association.getIsDateEnable()).isTrue();
        assertThat(association.getDateIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should disable date column")
    void shouldDisableDateColumn() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsDateEnable(false);

        assertThat(association.getIsDateEnable()).isFalse();
    }

    @Test
    @DisplayName("Should enable hour column")
    void shouldEnableHourColumn() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsHourEnable(true);
        association.setHourIndex(1);

        assertThat(association.getIsHourEnable()).isTrue();
        assertThat(association.getHourIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should disable hour column")
    void shouldDisableHourColumn() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsHourEnable(false);

        assertThat(association.getIsHourEnable()).isFalse();
    }

    @Test
    @DisplayName("Should enable user column")
    void shouldEnableUserColumn() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsUserEnable(true);
        association.setUserIndex(2);

        assertThat(association.getIsUserEnable()).isTrue();
        assertThat(association.getUserIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should disable user column")
    void shouldDisableUserColumn() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsUserEnable(false);

        assertThat(association.getIsUserEnable()).isFalse();
    }

    @Test
    @DisplayName("Should enable read column")
    void shouldEnableReadColumn() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsReadEnable(true);

        assertThat(association.getIsReadEnable()).isTrue();
    }

    @Test
    @DisplayName("Should disable read column")
    void shouldDisableReadColumn() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsReadEnable(false);

        assertThat(association.getIsReadEnable()).isFalse();
    }

    @Test
    @DisplayName("Should support zero-based index for date")
    void shouldSupportZeroBasedIndexForDate() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setDateIndex(0);

        assertThat(association.getDateIndex()).isZero();
    }

    @Test
    @DisplayName("Should support sequential column indexes")
    void shouldSupportSequentialColumnIndexes() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setDateIndex(0);
        association.setHourIndex(1);
        association.setUserIndex(2);

        assertThat(association.getDateIndex()).isEqualTo(0);
        assertThat(association.getHourIndex()).isEqualTo(1);
        assertThat(association.getUserIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should support non-sequential column indexes")
    void shouldSupportNonSequentialColumnIndexes() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setDateIndex(0);
        association.setHourIndex(2);
        association.setUserIndex(5);

        assertThat(association.getDateIndex()).isLessThan(association.getHourIndex());
        assertThat(association.getHourIndex()).isLessThan(association.getUserIndex());
    }

    @Test
    @DisplayName("Should allow null indexes when columns are disabled")
    void shouldAllowNullIndexesWhenColumnsAreDisabled() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsDateEnable(false);
        association.setDateIndex(null);

        assertThat(association.getIsDateEnable()).isFalse();
        assertThat(association.getDateIndex()).isNull();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of output associations")
    void shouldMaintainOneToManyCollectionOfOutputAssociations() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setOutputAssociations(new HashSet<>());

        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setId(1L);
        association.getOutputAssociations().add(outputAssociation);

        assertThat(association.getOutputAssociations())
                .isNotNull()
                .hasSize(1)
                .contains(outputAssociation);
    }

    @Test
    @DisplayName("Should support multiple output associations")
    void shouldSupportMultipleOutputAssociations() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setOutputAssociations(new HashSet<>());

        InstrumentTabulateOutputAssociationEntity output1 = new InstrumentTabulateOutputAssociationEntity();
        output1.setId(1L);
        InstrumentTabulateOutputAssociationEntity output2 = new InstrumentTabulateOutputAssociationEntity();
        output2.setId(2L);

        association.getOutputAssociations().add(output1);
        association.getOutputAssociations().add(output2);

        assertThat(association.getOutputAssociations()).hasSize(2);
    }

    @Test
    @DisplayName("Should initialize empty output associations collection by default")
    void shouldInitializeEmptyOutputAssociationsCollectionByDefault() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();

        assertThat(association.getOutputAssociations()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing output associations")
    void shouldSupportAddingAndRemovingOutputAssociations() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        InstrumentTabulateOutputAssociationEntity output = new InstrumentTabulateOutputAssociationEntity();
        output.setId(1L);

        association.getOutputAssociations().add(output);
        assertThat(association.getOutputAssociations()).hasSize(1);

        association.getOutputAssociations().remove(output);

        assertThat(association.getOutputAssociations()).isEmpty();
    }

    @Test
    @DisplayName("Should allow multiple associations per pattern")
    void shouldAllowMultipleAssociationsPerPattern() {

        InstrumentEntity instrument2 = new InstrumentEntity();
        instrument2.setId(2L);
        instrument2.setName("Instrumento 2");

        InstrumentTabulateAssociationEntity association1 = new InstrumentTabulateAssociationEntity();
        association1.setId(1L);
        association1.setPattern(pattern);
        association1.setInstrument(instrument);

        InstrumentTabulateAssociationEntity association2 = new InstrumentTabulateAssociationEntity();
        association2.setId(2L);
        association2.setPattern(pattern);
        association2.setInstrument(instrument2);

        assertThat(association1.getPattern()).isEqualTo(association2.getPattern());
        assertThat(association1.getId()).isNotEqualTo(association2.getId());
        assertThat(association1.getInstrument()).isNotEqualTo(association2.getInstrument());
    }

    @Test
    @DisplayName("Should support selective column enablement")
    void shouldSupportSelectiveColumnEnablement() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsDateEnable(true);
        association.setIsHourEnable(false);
        association.setIsUserEnable(true);
        association.setIsReadEnable(false);

        assertThat(association.getIsDateEnable()).isTrue();
        assertThat(association.getIsHourEnable()).isFalse();
        assertThat(association.getIsUserEnable()).isTrue();
        assertThat(association.getIsReadEnable()).isFalse();
    }

    @Test
    @DisplayName("Should support all columns enabled")
    void shouldSupportAllColumnsEnabled() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsDateEnable(true);
        association.setIsHourEnable(true);
        association.setIsUserEnable(true);
        association.setIsReadEnable(true);

        assertThat(association.getIsDateEnable()).isTrue();
        assertThat(association.getIsHourEnable()).isTrue();
        assertThat(association.getIsUserEnable()).isTrue();
        assertThat(association.getIsReadEnable()).isTrue();
    }

    @Test
    @DisplayName("Should support all columns disabled")
    void shouldSupportAllColumnsDisabled() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setIsDateEnable(false);
        association.setIsHourEnable(false);
        association.setIsUserEnable(false);
        association.setIsReadEnable(false);

        assertThat(association.getIsDateEnable()).isFalse();
        assertThat(association.getIsHourEnable()).isFalse();
        assertThat(association.getIsUserEnable()).isFalse();
        assertThat(association.getIsReadEnable()).isFalse();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(1L);
        association.setIsDateEnable(false);

        Long originalId = association.getId();

        association.setIsDateEnable(true);
        association.setDateIndex(0);

        assertThat(association.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support cascade operations on output associations")
    void shouldSupportCascadeOperationsOnOutputAssociations() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        InstrumentTabulateOutputAssociationEntity output = new InstrumentTabulateOutputAssociationEntity();
        output.setId(1L);
        output.setAssociation(association);

        association.getOutputAssociations().add(output);

        assertThat(output.getAssociation()).isEqualTo(association);
        assertThat(association.getOutputAssociations()).contains(output);
    }

    @Test
    @DisplayName("Should support orphan removal for output associations")
    void shouldSupportOrphanRemovalForOutputAssociations() {

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        InstrumentTabulateOutputAssociationEntity output = new InstrumentTabulateOutputAssociationEntity();
        output.setId(1L);
        association.getOutputAssociations().add(output);

        association.getOutputAssociations().remove(output);

        assertThat(association.getOutputAssociations()).doesNotContain(output);
    }
}
