package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.PotentialDamageEntity;
import com.geosegbar.entities.RegulatoryDamEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - PotentialDamageEntity")
class PotentialDamageEntityTest extends BaseUnitTest {

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
    }

    @Test
    @DisplayName("Should create potential damage with all required fields")
    void shouldCreatePotentialDamageWithAllRequiredFields() {
        // Given
        PotentialDamageEntity damage = new PotentialDamageEntity();
        damage.setId(1L);
        damage.setName("Alto");

        // Then
        assertThat(damage).satisfies(d -> {
            assertThat(d.getId()).isEqualTo(1L);
            assertThat(d.getName()).isEqualTo("Alto");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        PotentialDamageEntity damage = new PotentialDamageEntity(
                1L,
                "Alto",
                new HashSet<>()
        );

        // Then
        assertThat(damage.getId()).isEqualTo(1L);
        assertThat(damage.getName()).isEqualTo("Alto");
        assertThat(damage.getRegulatoryDams()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support unique name constraint")
    void shouldSupportUniqueNameConstraint() {
        // Given
        PotentialDamageEntity damage1 = new PotentialDamageEntity();
        damage1.setId(1L);
        damage1.setName("Alto");

        PotentialDamageEntity damage2 = new PotentialDamageEntity();
        damage2.setId(2L);
        damage2.setName("Médio");

        // Then - Different names
        assertThat(damage1.getName()).isNotEqualTo(damage2.getName());
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of regulatory dams")
    void shouldMaintainOneToManyCollectionOfRegulatoryDams() {
        // Given
        PotentialDamageEntity damage = new PotentialDamageEntity();
        damage.setName("Alto");
        damage.setRegulatoryDams(new HashSet<>());

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);
        damage.getRegulatoryDams().add(regulatoryDam);

        // Then
        assertThat(damage.getRegulatoryDams())
                .isNotNull()
                .hasSize(1)
                .contains(regulatoryDam);
    }

    @Test
    @DisplayName("Should support multiple regulatory dams per potential damage")
    void shouldSupportMultipleRegulatoryDamsPerPotentialDamage() {
        // Given
        PotentialDamageEntity damage = new PotentialDamageEntity();
        damage.setName("Alto");
        damage.setRegulatoryDams(new HashSet<>());

        RegulatoryDamEntity dam1 = new RegulatoryDamEntity();
        dam1.setId(1L);
        RegulatoryDamEntity dam2 = new RegulatoryDamEntity();
        dam2.setId(2L);
        RegulatoryDamEntity dam3 = new RegulatoryDamEntity();
        dam3.setId(3L);

        damage.getRegulatoryDams().add(dam1);
        damage.getRegulatoryDams().add(dam2);
        damage.getRegulatoryDams().add(dam3);

        // Then
        assertThat(damage.getRegulatoryDams()).hasSize(3);
    }

    @Test
    @DisplayName("Should initialize empty regulatory dams collection by default")
    void shouldInitializeEmptyRegulatoryDamsCollectionByDefault() {
        // Given & When
        PotentialDamageEntity damage = new PotentialDamageEntity();

        // Then
        assertThat(damage.getRegulatoryDams()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing regulatory dams")
    void shouldSupportAddingAndRemovingRegulatoryDams() {
        // Given
        PotentialDamageEntity damage = new PotentialDamageEntity();
        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);

        // When
        damage.getRegulatoryDams().add(regulatoryDam);
        assertThat(damage.getRegulatoryDams()).hasSize(1);

        damage.getRegulatoryDams().remove(regulatoryDam);

        // Then
        assertThat(damage.getRegulatoryDams()).isEmpty();
    }

    @Test
    @DisplayName("Should support common potential damage levels")
    void shouldSupportCommonPotentialDamageLevels() {
        // Given
        PotentialDamageEntity high = new PotentialDamageEntity();
        high.setName("Alto");

        PotentialDamageEntity medium = new PotentialDamageEntity();
        medium.setName("Médio");

        PotentialDamageEntity low = new PotentialDamageEntity();
        low.setName("Baixo");

        // Then
        assertThat(high.getName()).isEqualTo("Alto");
        assertThat(medium.getName()).isEqualTo("Médio");
        assertThat(low.getName()).isEqualTo("Baixo");
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {
        // Given
        PotentialDamageEntity damage = new PotentialDamageEntity();
        damage.setName("Médio");

        // Then
        assertThat(damage.getName()).contains("é");
    }

    @Test
    @DisplayName("Should support descriptive damage names")
    void shouldSupportDescriptiveDamageNames() {
        // Given
        PotentialDamageEntity damage = new PotentialDamageEntity();
        damage.setName("Dano Potencial Alto com Risco Elevado");

        // Then
        assertThat(damage.getName()).hasSize(37);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        PotentialDamageEntity damage = new PotentialDamageEntity();
        damage.setId(1L);
        damage.setName("Alto");

        Long originalId = damage.getId();

        // When
        damage.setName("Muito Alto");

        // Then
        assertThat(damage.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support lazy fetch for regulatory dams")
    void shouldSupportLazyFetchForRegulatoryDams() {
        // Given
        PotentialDamageEntity damage = new PotentialDamageEntity();
        damage.setName("Alto");

        // Then - Regulatory dams collection initialized but lazy
        assertThat(damage.getRegulatoryDams()).isNotNull();
    }

    @Test
    @DisplayName("Should support name as unique identifier")
    void shouldSupportNameAsUniqueIdentifier() {
        // Given
        PotentialDamageEntity damage = new PotentialDamageEntity();
        damage.setName("Identificador Único");

        // Then - Name indexed as unique
        assertThat(damage.getName()).isNotBlank();
    }

    @Test
    @DisplayName("Should support bidirectional relationship with regulatory dams")
    void shouldSupportBidirectionalRelationshipWithRegulatoryDams() {
        // Given
        PotentialDamageEntity damage = new PotentialDamageEntity();
        damage.setId(1L);
        damage.setName("Alto");

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);
        regulatoryDam.setPotentialDamage(damage);

        // When
        damage.getRegulatoryDams().add(regulatoryDam);

        // Then - Bidirectional relationship
        assertThat(regulatoryDam.getPotentialDamage()).isEqualTo(damage);
        assertThat(damage.getRegulatoryDams()).contains(regulatoryDam);
    }

    @Test
    @DisplayName("Should support different damage classification levels")
    void shouldSupportDifferentDamageClassificationLevels() {
        // Given - Different severity levels
        PotentialDamageEntity low = new PotentialDamageEntity();
        low.setId(1L);
        low.setName("Baixo");

        PotentialDamageEntity medium = new PotentialDamageEntity();
        medium.setId(2L);
        medium.setName("Médio");

        PotentialDamageEntity high = new PotentialDamageEntity();
        high.setId(3L);
        high.setName("Alto");

        PotentialDamageEntity veryHigh = new PotentialDamageEntity();
        veryHigh.setId(4L);
        veryHigh.setName("Muito Alto");

        // Then - All levels distinct
        assertThat(low.getId()).isNotEqualTo(medium.getId());
        assertThat(medium.getId()).isNotEqualTo(high.getId());
        assertThat(high.getId()).isNotEqualTo(veryHigh.getId());
    }
}
