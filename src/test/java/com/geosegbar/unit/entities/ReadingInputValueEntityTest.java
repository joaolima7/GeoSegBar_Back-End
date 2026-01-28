package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ReadingEntity;
import com.geosegbar.entities.ReadingInputValueEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - ReadingInputValueEntity")
class ReadingInputValueEntityTest extends BaseUnitTest {

    private ReadingEntity reading;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();

        reading = new ReadingEntity();
        reading.setId(1L);
    }

    @Test
    @DisplayName("Should create reading input value with all required fields")
    void shouldCreateReadingInputValueWithAllRequiredFields() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setId(1L);
        inputValue.setInputAcronym("X");
        inputValue.setInputName("Cota");
        inputValue.setValue(BigDecimal.valueOf(125.5));
        inputValue.setReading(reading);

        assertThat(inputValue).satisfies(iv -> {
            assertThat(iv.getId()).isEqualTo(1L);
            assertThat(iv.getInputAcronym()).isEqualTo("X");
            assertThat(iv.getInputName()).isEqualTo("Cota");
            assertThat(iv.getValue()).isEqualByComparingTo(BigDecimal.valueOf(125.5));
            assertThat(iv.getReading()).isEqualTo(reading);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity(
                1L,
                "Y",
                "Pressão",
                BigDecimal.valueOf(50.2),
                reading
        );

        assertThat(inputValue.getId()).isEqualTo(1L);
        assertThat(inputValue.getInputAcronym()).isEqualTo("Y");
        assertThat(inputValue.getInputName()).isEqualTo("Pressão");
        assertThat(inputValue.getValue()).isEqualByComparingTo(BigDecimal.valueOf(50.2));
        assertThat(inputValue.getReading()).isEqualTo(reading);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Reading")
    void shouldMaintainManyToOneRelationshipWithReading() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setReading(reading);

        assertThat(inputValue.getReading())
                .isNotNull()
                .isEqualTo(reading);
    }

    @Test
    @DisplayName("Should support single character acronyms")
    void shouldSupportSingleCharacterAcronyms() {

        ReadingInputValueEntity input1 = new ReadingInputValueEntity();
        input1.setInputAcronym("X");

        ReadingInputValueEntity input2 = new ReadingInputValueEntity();
        input2.setInputAcronym("Y");

        ReadingInputValueEntity input3 = new ReadingInputValueEntity();
        input3.setInputAcronym("Z");

        assertThat(input1.getInputAcronym()).isEqualTo("X");
        assertThat(input2.getInputAcronym()).isEqualTo("Y");
        assertThat(input3.getInputAcronym()).isEqualTo("Z");
    }

    @Test
    @DisplayName("Should support multi-character acronyms")
    void shouldSupportMultiCharacterAcronyms() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setInputAcronym("COTA");

        assertThat(inputValue.getInputAcronym()).isEqualTo("COTA");
    }

    @Test
    @DisplayName("Should support Portuguese characters in input name")
    void shouldSupportPortugueseCharactersInInputName() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setInputName("Pressão");

        assertThat(inputValue.getInputName()).contains("ã");
    }

    @Test
    @DisplayName("Should support descriptive input names")
    void shouldSupportDescriptiveInputNames() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setInputName("Nível de água do reservatório");

        assertThat(inputValue.getInputName()).hasSize(29);
    }

    @Test
    @DisplayName("Should support positive values")
    void shouldSupportPositiveValues() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setValue(BigDecimal.valueOf(100.5));

        assertThat(inputValue.getValue()).isEqualByComparingTo(BigDecimal.valueOf(100.5));
    }

    @Test
    @DisplayName("Should support negative values")
    void shouldSupportNegativeValues() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setValue(BigDecimal.valueOf(-25.3));

        assertThat(inputValue.getValue()).isEqualByComparingTo(BigDecimal.valueOf(-25.3));
    }

    @Test
    @DisplayName("Should support zero value")
    void shouldSupportZeroValue() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setValue(BigDecimal.valueOf(0.0));

        assertThat(inputValue.getValue()).isZero();
    }

    @Test
    @DisplayName("Should support high precision values")
    void shouldSupportHighPrecisionValues() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setValue(BigDecimal.valueOf(123.456789));

        assertThat(inputValue.getValue()).isEqualByComparingTo(BigDecimal.valueOf(123.456789));
    }

    @Test
    @DisplayName("Should support very small decimal values")
    void shouldSupportVerySmallDecimalValues() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setValue(BigDecimal.valueOf(0.0001));

        assertThat(inputValue.getValue()).isEqualByComparingTo(BigDecimal.valueOf(0.0001));
    }

    @Test
    @DisplayName("Should support large values")
    void shouldSupportLargeValues() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setValue(BigDecimal.valueOf(9999.99));

        assertThat(inputValue.getValue()).isEqualByComparingTo(BigDecimal.valueOf(9999.99));
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setId(1L);
        inputValue.setValue(BigDecimal.valueOf(10.0));

        Long originalId = inputValue.getId();

        inputValue.setValue(BigDecimal.valueOf(20.0));

        assertThat(inputValue.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support multiple input values per reading")
    void shouldSupportMultipleInputValuesPerReading() {

        ReadingInputValueEntity input1 = new ReadingInputValueEntity();
        input1.setId(1L);
        input1.setInputAcronym("X");
        input1.setReading(reading);

        ReadingInputValueEntity input2 = new ReadingInputValueEntity();
        input2.setId(2L);
        input2.setInputAcronym("Y");
        input2.setReading(reading);

        ReadingInputValueEntity input3 = new ReadingInputValueEntity();
        input3.setId(3L);
        input3.setInputAcronym("Z");
        input3.setReading(reading);

        assertThat(input1.getReading()).isEqualTo(input2.getReading());
        assertThat(input2.getReading()).isEqualTo(input3.getReading());
    }

    @Test
    @DisplayName("Should support common measurement acronyms")
    void shouldSupportCommonMeasurementAcronyms() {

        ReadingInputValueEntity x = new ReadingInputValueEntity();
        x.setInputAcronym("X");
        x.setInputName("Cota");

        ReadingInputValueEntity y = new ReadingInputValueEntity();
        y.setInputAcronym("Y");
        y.setInputName("Pressão");

        ReadingInputValueEntity t = new ReadingInputValueEntity();
        t.setInputAcronym("T");
        t.setInputName("Temperatura");

        assertThat(x.getInputAcronym()).isEqualTo("X");
        assertThat(y.getInputAcronym()).isEqualTo("Y");
        assertThat(t.getInputAcronym()).isEqualTo("T");
    }

    @Test
    @DisplayName("Should support equation variable names")
    void shouldSupportEquationVariableNames() {

        ReadingInputValueEntity x = new ReadingInputValueEntity();
        x.setInputAcronym("X");
        x.setValue(BigDecimal.valueOf(10.0));

        ReadingInputValueEntity y = new ReadingInputValueEntity();
        y.setInputAcronym("Y");
        y.setValue(BigDecimal.valueOf(6.0));

        ReadingInputValueEntity z = new ReadingInputValueEntity();
        z.setInputAcronym("Z");
        z.setValue(BigDecimal.valueOf(1.0));

        assertThat(x.getValue()).isEqualTo(10.0);
        assertThat(y.getValue()).isEqualTo(6.0);
        assertThat(z.getValue()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should support bidirectional relationship with reading")
    void shouldSupportBidirectionalRelationshipWithReading() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setId(1L);
        inputValue.setInputAcronym("X");
        inputValue.setReading(reading);

        reading.getInputValues().add(inputValue);

        assertThat(inputValue.getReading()).isEqualTo(reading);
        assertThat(reading.getInputValues()).contains(inputValue);
    }

    @Test
    @DisplayName("Should support input value with uppercase acronym")
    void shouldSupportInputValueWithUppercaseAcronym() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setInputAcronym("DELTA");

        assertThat(inputValue.getInputAcronym()).isEqualTo("DELTA");
    }

    @Test
    @DisplayName("Should support Greek letters in acronym")
    void shouldSupportGreekLettersInAcronym() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setInputAcronym("α");

        assertThat(inputValue.getInputAcronym()).isEqualTo("α");
    }

    @Test
    @DisplayName("Should support subscript notation in acronym")
    void shouldSupportSubscriptNotationInAcronym() {

        ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
        inputValue.setInputAcronym("X1");

        assertThat(inputValue.getInputAcronym()).contains("1");
    }

    @Test
    @DisplayName("Should differentiate between similar acronyms")
    void shouldDifferentiateBetweenSimilarAcronyms() {

        ReadingInputValueEntity x = new ReadingInputValueEntity();
        x.setInputAcronym("X");

        ReadingInputValueEntity x1 = new ReadingInputValueEntity();
        x1.setInputAcronym("X1");

        ReadingInputValueEntity x2 = new ReadingInputValueEntity();
        x2.setInputAcronym("X2");

        assertThat(x.getInputAcronym()).isNotEqualTo(x1.getInputAcronym());
        assertThat(x1.getInputAcronym()).isNotEqualTo(x2.getInputAcronym());
    }
}
