package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.InstrumentTabulateAssociationEntity;
import com.geosegbar.entities.InstrumentTabulateOutputAssociationEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentTabulateOutputAssociationEntity")
class InstrumentTabulateOutputAssociationEntityTest extends BaseUnitTest {

    private InstrumentTabulateAssociationEntity association;
    private OutputEntity output;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        association = new InstrumentTabulateAssociationEntity();
        association.setId(1L);

        output = new OutputEntity();
        output.setId(1L);
        output.setName("Output 1");
    }

    @Test
    @DisplayName("Should create output association with all required fields")
    void shouldCreateOutputAssociationWithAllRequiredFields() {
        // Given
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setId(1L);
        outputAssociation.setAssociation(association);
        outputAssociation.setOutput(output);
        outputAssociation.setOutputIndex(0);

        // Then
        assertThat(outputAssociation).satisfies(oa -> {
            assertThat(oa.getId()).isEqualTo(1L);
            assertThat(oa.getAssociation()).isEqualTo(association);
            assertThat(oa.getOutput()).isEqualTo(output);
            assertThat(oa.getOutputIndex()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity(
                1L,
                association,
                output,
                0
        );

        // Then
        assertThat(outputAssociation.getId()).isEqualTo(1L);
        assertThat(outputAssociation.getAssociation()).isEqualTo(association);
        assertThat(outputAssociation.getOutput()).isEqualTo(output);
        assertThat(outputAssociation.getOutputIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with InstrumentTabulateAssociation")
    void shouldMaintainManyToOneRelationshipWithInstrumentTabulateAssociation() {
        // Given
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setAssociation(association);

        // Then
        assertThat(outputAssociation.getAssociation())
                .isNotNull()
                .isEqualTo(association);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Output")
    void shouldMaintainManyToOneRelationshipWithOutput() {
        // Given
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setOutput(output);

        // Then
        assertThat(outputAssociation.getOutput())
                .isNotNull()
                .isEqualTo(output);
    }

    @Test
    @DisplayName("Should support zero-based index for first output")
    void shouldSupportZeroBasedIndexForFirstOutput() {
        // Given
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setOutputIndex(0);

        // Then
        assertThat(outputAssociation.getOutputIndex()).isZero();
    }

    @Test
    @DisplayName("Should support sequential output indexes")
    void shouldSupportSequentialOutputIndexes() {
        // Given
        InstrumentTabulateOutputAssociationEntity output1 = new InstrumentTabulateOutputAssociationEntity();
        output1.setId(1L);
        output1.setOutputIndex(0);

        InstrumentTabulateOutputAssociationEntity output2 = new InstrumentTabulateOutputAssociationEntity();
        output2.setId(2L);
        output2.setOutputIndex(1);

        InstrumentTabulateOutputAssociationEntity output3 = new InstrumentTabulateOutputAssociationEntity();
        output3.setId(3L);
        output3.setOutputIndex(2);

        // Then
        assertThat(output1.getOutputIndex()).isEqualTo(0);
        assertThat(output2.getOutputIndex()).isEqualTo(1);
        assertThat(output3.getOutputIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should support non-sequential output indexes")
    void shouldSupportNonSequentialOutputIndexes() {
        // Given
        InstrumentTabulateOutputAssociationEntity output1 = new InstrumentTabulateOutputAssociationEntity();
        output1.setOutputIndex(0);

        InstrumentTabulateOutputAssociationEntity output2 = new InstrumentTabulateOutputAssociationEntity();
        output2.setOutputIndex(3);

        InstrumentTabulateOutputAssociationEntity output3 = new InstrumentTabulateOutputAssociationEntity();
        output3.setOutputIndex(7);

        // Then
        assertThat(output1.getOutputIndex()).isLessThan(output2.getOutputIndex());
        assertThat(output2.getOutputIndex()).isLessThan(output3.getOutputIndex());
    }

    @Test
    @DisplayName("Should support large index values")
    void shouldSupportLargeIndexValues() {
        // Given
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setOutputIndex(99);

        // Then
        assertThat(outputAssociation.getOutputIndex()).isEqualTo(99);
    }

    @Test
    @DisplayName("Should allow multiple output associations per instrument association")
    void shouldAllowMultipleOutputAssociationsPerInstrumentAssociation() {
        // Given
        OutputEntity output2 = new OutputEntity();
        output2.setId(2L);
        output2.setName("Output 2");

        InstrumentTabulateOutputAssociationEntity outputAssoc1 = new InstrumentTabulateOutputAssociationEntity();
        outputAssoc1.setId(1L);
        outputAssoc1.setAssociation(association);
        outputAssoc1.setOutput(output);
        outputAssoc1.setOutputIndex(0);

        InstrumentTabulateOutputAssociationEntity outputAssoc2 = new InstrumentTabulateOutputAssociationEntity();
        outputAssoc2.setId(2L);
        outputAssoc2.setAssociation(association);
        outputAssoc2.setOutput(output2);
        outputAssoc2.setOutputIndex(1);

        // Then
        assertThat(outputAssoc1.getAssociation()).isEqualTo(outputAssoc2.getAssociation());
        assertThat(outputAssoc1.getId()).isNotEqualTo(outputAssoc2.getId());
        assertThat(outputAssoc1.getOutput()).isNotEqualTo(outputAssoc2.getOutput());
        assertThat(outputAssoc1.getOutputIndex()).isNotEqualTo(outputAssoc2.getOutputIndex());
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setId(1L);
        outputAssociation.setOutputIndex(0);

        Long originalId = outputAssociation.getId();

        // When
        outputAssociation.setOutputIndex(5);

        // Then
        assertThat(outputAssociation.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support output index reordering")
    void shouldSupportOutputIndexReordering() {
        // Given
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setOutputIndex(0);

        // When
        outputAssociation.setOutputIndex(3);

        // Then
        assertThat(outputAssociation.getOutputIndex()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should reference parent association correctly")
    void shouldReferenceParentAssociationCorrectly() {
        // Given
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setAssociation(association);

        // When - Add to parent collection
        association.getOutputAssociations().add(outputAssociation);

        // Then
        assertThat(outputAssociation.getAssociation()).isEqualTo(association);
        assertThat(association.getOutputAssociations()).contains(outputAssociation);
    }

    @Test
    @DisplayName("Should support different outputs for same association")
    void shouldSupportDifferentOutputsForSameAssociation() {
        // Given
        OutputEntity output2 = new OutputEntity();
        output2.setId(2L);
        output2.setName("Output 2");

        OutputEntity output3 = new OutputEntity();
        output3.setId(3L);
        output3.setName("Output 3");

        InstrumentTabulateOutputAssociationEntity outputAssoc1 = new InstrumentTabulateOutputAssociationEntity();
        outputAssoc1.setAssociation(association);
        outputAssoc1.setOutput(output);
        outputAssoc1.setOutputIndex(0);

        InstrumentTabulateOutputAssociationEntity outputAssoc2 = new InstrumentTabulateOutputAssociationEntity();
        outputAssoc2.setAssociation(association);
        outputAssoc2.setOutput(output2);
        outputAssoc2.setOutputIndex(1);

        InstrumentTabulateOutputAssociationEntity outputAssoc3 = new InstrumentTabulateOutputAssociationEntity();
        outputAssoc3.setAssociation(association);
        outputAssoc3.setOutput(output3);
        outputAssoc3.setOutputIndex(2);

        // Then
        assertThat(outputAssoc1.getAssociation()).isEqualTo(association);
        assertThat(outputAssoc2.getAssociation()).isEqualTo(association);
        assertThat(outputAssoc3.getAssociation()).isEqualTo(association);
        assertThat(outputAssoc1.getOutput()).isNotEqualTo(outputAssoc2.getOutput());
        assertThat(outputAssoc2.getOutput()).isNotEqualTo(outputAssoc3.getOutput());
    }

    @Test
    @DisplayName("Should maintain bidirectional relationship with association")
    void shouldMaintainBidirectionalRelationshipWithAssociation() {
        // Given
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setId(1L);
        outputAssociation.setAssociation(association);
        outputAssociation.setOutput(output);
        outputAssociation.setOutputIndex(0);

        // When
        association.getOutputAssociations().add(outputAssociation);

        // Then - Bidirectional relationship established
        assertThat(outputAssociation.getAssociation()).isEqualTo(association);
        assertThat(association.getOutputAssociations())
                .isNotNull()
                .hasSize(1)
                .contains(outputAssociation);
    }

    @Test
    @DisplayName("Should support orphan removal when removed from parent collection")
    void shouldSupportOrphanRemovalWhenRemovedFromParentCollection() {
        // Given
        InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
        outputAssociation.setId(1L);
        outputAssociation.setAssociation(association);
        association.getOutputAssociations().add(outputAssociation);

        // When - Remove from parent collection
        association.getOutputAssociations().remove(outputAssociation);

        // Then - No longer in parent collection
        assertThat(association.getOutputAssociations()).doesNotContain(outputAssociation);
    }

    @Test
    @DisplayName("Should support index-based column ordering concept")
    void shouldSupportIndexBasedColumnOrderingConcept() {
        // Given - Outputs with specific column positions
        InstrumentTabulateOutputAssociationEntity pressure = new InstrumentTabulateOutputAssociationEntity();
        pressure.setOutputIndex(0); // First column after fixed columns

        InstrumentTabulateOutputAssociationEntity temperature = new InstrumentTabulateOutputAssociationEntity();
        temperature.setOutputIndex(1); // Second column

        InstrumentTabulateOutputAssociationEntity flow = new InstrumentTabulateOutputAssociationEntity();
        flow.setOutputIndex(2); // Third column

        // Then - Indexes define display order
        assertThat(pressure.getOutputIndex()).isLessThan(temperature.getOutputIndex());
        assertThat(temperature.getOutputIndex()).isLessThan(flow.getOutputIndex());
    }
}
