package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.InstrumentGraphAxesEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentGraphAxesEntity")
class InstrumentGraphAxesEntityTest extends BaseUnitTest {

    private InstrumentGraphPatternEntity pattern;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        pattern = new InstrumentGraphPatternEntity();
        pattern.setId(1L);
        pattern.setName("Pattern 1");
    }

    @Test
    @DisplayName("Should create graph axes with all required fields")
    void shouldCreateGraphAxesWithAllRequiredFields() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setId(1L);
        axes.setPattern(pattern);
        axes.setAbscissaPx(12);
        axes.setAbscissaGridLinesEnable(true);
        axes.setPrimaryOrdinatePx(12);
        axes.setSecondaryOrdinatePx(10);
        axes.setPrimaryOrdinateGridLinesEnable(true);

        assertThat(axes).satisfies(a -> {
            assertThat(a.getId()).isEqualTo(1L);
            assertThat(a.getPattern()).isEqualTo(pattern);
            assertThat(a.getAbscissaPx()).isEqualTo(12);
            assertThat(a.getAbscissaGridLinesEnable()).isTrue();
            assertThat(a.getPrimaryOrdinatePx()).isEqualTo(12);
            assertThat(a.getSecondaryOrdinatePx()).isEqualTo(10);
            assertThat(a.getPrimaryOrdinateGridLinesEnable()).isTrue();
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity(
                1L,
                pattern,
                12,
                true,
                12,
                10,
                true,
                "Tempo (dias)",
                "Temperatura (°C)",
                10.0,
                5.0,
                0.0,
                0.0,
                100.0,
                50.0
        );

        assertThat(axes.getId()).isEqualTo(1L);
        assertThat(axes.getPattern()).isEqualTo(pattern);
        assertThat(axes.getAbscissaPx()).isEqualTo(12);
        assertThat(axes.getAbscissaGridLinesEnable()).isTrue();
        assertThat(axes.getPrimaryOrdinatePx()).isEqualTo(12);
        assertThat(axes.getSecondaryOrdinatePx()).isEqualTo(10);
        assertThat(axes.getPrimaryOrdinateGridLinesEnable()).isTrue();
        assertThat(axes.getPrimaryOrdinateTitle()).isEqualTo("Tempo (dias)");
        assertThat(axes.getSecondaryOrdinateTitle()).isEqualTo("Temperatura (°C)");
        assertThat(axes.getPrimaryOrdinateSpacing()).isEqualTo(10.0);
        assertThat(axes.getSecondaryOrdinateSpacing()).isEqualTo(5.0);
        assertThat(axes.getPrimaryOrdinateInitialValue()).isEqualTo(0.0);
        assertThat(axes.getSecondaryOrdinateInitialValue()).isEqualTo(0.0);
        assertThat(axes.getPrimaryOrdinateMaximumValue()).isEqualTo(100.0);
        assertThat(axes.getSecondaryOrdinateMaximumValue()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with InstrumentGraphPattern")
    void shouldMaintainOneToOneRelationshipWithInstrumentGraphPattern() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPattern(pattern);

        assertThat(axes.getPattern())
                .isNotNull()
                .isEqualTo(pattern);
    }

    @Test
    @DisplayName("Should support small font sizes")
    void shouldSupportSmallFontSizes() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setAbscissaPx(8);
        axes.setPrimaryOrdinatePx(8);
        axes.setSecondaryOrdinatePx(8);

        assertThat(axes.getAbscissaPx()).isEqualTo(8);
        assertThat(axes.getPrimaryOrdinatePx()).isEqualTo(8);
        assertThat(axes.getSecondaryOrdinatePx()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should support large font sizes")
    void shouldSupportLargeFontSizes() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setAbscissaPx(24);
        axes.setPrimaryOrdinatePx(20);
        axes.setSecondaryOrdinatePx(18);

        assertThat(axes.getAbscissaPx()).isEqualTo(24);
        assertThat(axes.getPrimaryOrdinatePx()).isEqualTo(20);
        assertThat(axes.getSecondaryOrdinatePx()).isEqualTo(18);
    }

    @Test
    @DisplayName("Should support different font sizes for each axis")
    void shouldSupportDifferentFontSizesForEachAxis() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setAbscissaPx(12);
        axes.setPrimaryOrdinatePx(14);
        axes.setSecondaryOrdinatePx(10);

        assertThat(axes.getAbscissaPx()).isNotEqualTo(axes.getPrimaryOrdinatePx());
        assertThat(axes.getPrimaryOrdinatePx()).isNotEqualTo(axes.getSecondaryOrdinatePx());
    }

    @Test
    @DisplayName("Should enable abscissa grid lines")
    void shouldEnableAbscissaGridLines() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setAbscissaGridLinesEnable(true);

        assertThat(axes.getAbscissaGridLinesEnable()).isTrue();
    }

    @Test
    @DisplayName("Should disable abscissa grid lines")
    void shouldDisableAbscissaGridLines() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setAbscissaGridLinesEnable(false);

        assertThat(axes.getAbscissaGridLinesEnable()).isFalse();
    }

    @Test
    @DisplayName("Should enable primary ordinate grid lines")
    void shouldEnablePrimaryOrdinateGridLines() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateGridLinesEnable(true);

        assertThat(axes.getPrimaryOrdinateGridLinesEnable()).isTrue();
    }

    @Test
    @DisplayName("Should support independent grid line configuration")
    void shouldSupportIndependentGridLineConfiguration() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setAbscissaGridLinesEnable(true);
        axes.setPrimaryOrdinateGridLinesEnable(false);

        assertThat(axes.getAbscissaGridLinesEnable()).isTrue();
        assertThat(axes.getPrimaryOrdinateGridLinesEnable()).isFalse();
    }

    @Test
    @DisplayName("Should support optional primary ordinate title")
    void shouldSupportOptionalPrimaryOrdinateTitle() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateTitle("Pressão (kPa)");

        assertThat(axes.getPrimaryOrdinateTitle()).isEqualTo("Pressão (kPa)");
    }

    @Test
    @DisplayName("Should support optional secondary ordinate title")
    void shouldSupportOptionalSecondaryOrdinateTitle() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setSecondaryOrdinateTitle("Volume (m³)");

        assertThat(axes.getSecondaryOrdinateTitle()).isEqualTo("Volume (m³)");
    }

    @Test
    @DisplayName("Should allow null ordinate titles")
    void shouldAllowNullOrdinateTitles() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateTitle(null);
        axes.setSecondaryOrdinateTitle(null);

        assertThat(axes.getPrimaryOrdinateTitle()).isNull();
        assertThat(axes.getSecondaryOrdinateTitle()).isNull();
    }

    @Test
    @DisplayName("Should support primary ordinate spacing")
    void shouldSupportPrimaryOrdinateSpacing() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateSpacing(10.0);

        assertThat(axes.getPrimaryOrdinateSpacing()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should support secondary ordinate spacing")
    void shouldSupportSecondaryOrdinateSpacing() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setSecondaryOrdinateSpacing(5.0);

        assertThat(axes.getSecondaryOrdinateSpacing()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should support different spacing for primary and secondary ordinates")
    void shouldSupportDifferentSpacingForPrimaryAndSecondaryOrdinates() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateSpacing(10.0);
        axes.setSecondaryOrdinateSpacing(2.5);

        assertThat(axes.getPrimaryOrdinateSpacing()).isNotEqualTo(axes.getSecondaryOrdinateSpacing());
    }

    @Test
    @DisplayName("Should support primary ordinate initial value")
    void shouldSupportPrimaryOrdinateInitialValue() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateInitialValue(0.0);

        assertThat(axes.getPrimaryOrdinateInitialValue()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should support negative initial values")
    void shouldSupportNegativeInitialValues() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateInitialValue(-10.0);
        axes.setSecondaryOrdinateInitialValue(-5.0);

        assertThat(axes.getPrimaryOrdinateInitialValue()).isNegative();
        assertThat(axes.getSecondaryOrdinateInitialValue()).isNegative();
    }

    @Test
    @DisplayName("Should support primary ordinate maximum value")
    void shouldSupportPrimaryOrdinateMaximumValue() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateMaximumValue(100.0);

        assertThat(axes.getPrimaryOrdinateMaximumValue()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should support secondary ordinate maximum value")
    void shouldSupportSecondaryOrdinateMaximumValue() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setSecondaryOrdinateMaximumValue(50.0);

        assertThat(axes.getSecondaryOrdinateMaximumValue()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should support range configuration with initial and maximum values")
    void shouldSupportRangeConfigurationWithInitialAndMaximumValues() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateInitialValue(0.0);
        axes.setPrimaryOrdinateMaximumValue(100.0);

        assertThat(axes.getPrimaryOrdinateInitialValue()).isLessThan(axes.getPrimaryOrdinateMaximumValue());
    }

    @Test
    @DisplayName("Should allow null spacing values")
    void shouldAllowNullSpacingValues() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateSpacing(null);
        axes.setSecondaryOrdinateSpacing(null);

        assertThat(axes.getPrimaryOrdinateSpacing()).isNull();
        assertThat(axes.getSecondaryOrdinateSpacing()).isNull();
    }

    @Test
    @DisplayName("Should allow null initial values")
    void shouldAllowNullInitialValues() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateInitialValue(null);
        axes.setSecondaryOrdinateInitialValue(null);

        assertThat(axes.getPrimaryOrdinateInitialValue()).isNull();
        assertThat(axes.getSecondaryOrdinateInitialValue()).isNull();
    }

    @Test
    @DisplayName("Should allow null maximum values")
    void shouldAllowNullMaximumValues() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateMaximumValue(null);
        axes.setSecondaryOrdinateMaximumValue(null);

        assertThat(axes.getPrimaryOrdinateMaximumValue()).isNull();
        assertThat(axes.getSecondaryOrdinateMaximumValue()).isNull();
    }

    @Test
    @DisplayName("Should support Portuguese characters in titles")
    void shouldSupportPortugueseCharactersInTitles() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPrimaryOrdinateTitle("Nível d'Água (m)");
        axes.setSecondaryOrdinateTitle("Vazão (m³/s)");

        assertThat(axes.getPrimaryOrdinateTitle()).contains("'", "Á");
        assertThat(axes.getSecondaryOrdinateTitle()).contains("ã", "³");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setId(1L);
        axes.setAbscissaPx(12);

        Long originalId = axes.getId();

        axes.setAbscissaPx(14);
        axes.setPrimaryOrdinatePx(16);

        assertThat(axes.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support complete axes configuration")
    void shouldSupportCompleteAxesConfiguration() {

        InstrumentGraphAxesEntity axes = new InstrumentGraphAxesEntity();
        axes.setPattern(pattern);
        axes.setAbscissaPx(12);
        axes.setAbscissaGridLinesEnable(true);
        axes.setPrimaryOrdinatePx(12);
        axes.setSecondaryOrdinatePx(10);
        axes.setPrimaryOrdinateGridLinesEnable(true);
        axes.setPrimaryOrdinateTitle("Eixo Y Principal");
        axes.setSecondaryOrdinateTitle("Eixo Y Secundário");
        axes.setPrimaryOrdinateSpacing(10.0);
        axes.setSecondaryOrdinateSpacing(5.0);
        axes.setPrimaryOrdinateInitialValue(0.0);
        axes.setSecondaryOrdinateInitialValue(0.0);
        axes.setPrimaryOrdinateMaximumValue(100.0);
        axes.setSecondaryOrdinateMaximumValue(50.0);

        assertThat(axes.getPattern()).isNotNull();
        assertThat(axes.getAbscissaPx()).isPositive();
        assertThat(axes.getPrimaryOrdinatePx()).isPositive();
        assertThat(axes.getSecondaryOrdinatePx()).isPositive();
        assertThat(axes.getPrimaryOrdinateTitle()).isNotBlank();
        assertThat(axes.getSecondaryOrdinateTitle()).isNotBlank();
        assertThat(axes.getPrimaryOrdinateSpacing()).isPositive();
        assertThat(axes.getSecondaryOrdinateSpacing()).isPositive();
    }
}
