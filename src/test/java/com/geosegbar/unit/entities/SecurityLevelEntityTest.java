package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.RegulatoryDamEntity;
import com.geosegbar.entities.SecurityLevelEntity;

@Tag("unit")
class SecurityLevelEntityTest extends BaseUnitTest {

    @Test
    @DisplayName("Should create security level with all required fields")
    void shouldCreateSecurityLevelWithAllRequiredFields() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setId(1L);
        securityLevel.setLevel("Alto");

        assertThat(securityLevel).satisfies(sl -> {
            assertThat(sl.getId()).isEqualTo(1L);
            assertThat(sl.getLevel()).isEqualTo("Alto");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity(1L, "Médio", null);

        assertThat(securityLevel.getId()).isEqualTo(1L);
        assertThat(securityLevel.getLevel()).isEqualTo("Médio");
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of regulatory dams")
    void shouldMaintainOneToManyCollectionOfRegulatoryDams() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setLevel("Alto");

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);
        regulatoryDam.setSecurityLevel(securityLevel);

        securityLevel.getRegulatoryDams().add(regulatoryDam);

        assertThat(securityLevel.getRegulatoryDams())
                .isNotNull()
                .hasSize(1)
                .contains(regulatoryDam);
    }

    @Test
    @DisplayName("Should initialize empty regulatory dams collection by default")
    void shouldInitializeEmptyRegulatoryDamsCollectionByDefault() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();

        assertThat(securityLevel.getRegulatoryDams()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support multiple regulatory dams per security level")
    void shouldSupportMultipleRegulatoryDamsPerSecurityLevel() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setLevel("Alto");

        RegulatoryDamEntity dam1 = new RegulatoryDamEntity();
        dam1.setId(1L);

        RegulatoryDamEntity dam2 = new RegulatoryDamEntity();
        dam2.setId(2L);

        RegulatoryDamEntity dam3 = new RegulatoryDamEntity();
        dam3.setId(3L);

        securityLevel.getRegulatoryDams().add(dam1);
        securityLevel.getRegulatoryDams().add(dam2);
        securityLevel.getRegulatoryDams().add(dam3);

        assertThat(securityLevel.getRegulatoryDams()).hasSize(3);
    }

    @Test
    @DisplayName("Should support common security level Baixo")
    void shouldSupportCommonSecurityLevelBaixo() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setLevel("Baixo");

        assertThat(securityLevel.getLevel()).isEqualTo("Baixo");
    }

    @Test
    @DisplayName("Should support common security level Médio")
    void shouldSupportCommonSecurityLevelMedio() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setLevel("Médio");

        assertThat(securityLevel.getLevel()).isEqualTo("Médio");
    }

    @Test
    @DisplayName("Should support common security level Alto")
    void shouldSupportCommonSecurityLevelAlto() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setLevel("Alto");

        assertThat(securityLevel.getLevel()).isEqualTo("Alto");
    }

    @Test
    @DisplayName("Should support Portuguese characters in level")
    void shouldSupportPortugueseCharactersInLevel() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setLevel("Nível de Segurança Máximo");

        assertThat(securityLevel.getLevel()).contains("í", "á");
    }

    @Test
    @DisplayName("Should support descriptive security level names")
    void shouldSupportDescriptiveSecurityLevelNames() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setLevel("Alto - Monitoramento Contínuo Necessário");

        assertThat(securityLevel.getLevel()).hasSize(40);
    }

    @Test
    @DisplayName("Should support short security level names")
    void shouldSupportShortSecurityLevelNames() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setLevel("A");

        assertThat(securityLevel.getLevel()).hasSize(1);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setId(1L);
        securityLevel.setLevel("Baixo");

        Long originalId = securityLevel.getId();

        securityLevel.setLevel("Alto");

        assertThat(securityLevel.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support different security classification levels")
    void shouldSupportDifferentSecurityClassificationLevels() {

        SecurityLevelEntity level1 = new SecurityLevelEntity();
        level1.setLevel("Baixo");

        SecurityLevelEntity level2 = new SecurityLevelEntity();
        level2.setLevel("Médio");

        SecurityLevelEntity level3 = new SecurityLevelEntity();
        level3.setLevel("Alto");

        SecurityLevelEntity level4 = new SecurityLevelEntity();
        level4.setLevel("Muito Alto");

        assertThat(level1.getLevel()).isNotEqualTo(level2.getLevel());
        assertThat(level2.getLevel()).isNotEqualTo(level3.getLevel());
        assertThat(level3.getLevel()).isNotEqualTo(level4.getLevel());
    }

    @Test
    @DisplayName("Should support bidirectional relationship with regulatory dams")
    void shouldSupportBidirectionalRelationshipWithRegulatoryDams() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        securityLevel.setId(1L);
        securityLevel.setLevel("Alto");

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);
        regulatoryDam.setSecurityLevel(securityLevel);

        securityLevel.getRegulatoryDams().add(regulatoryDam);

        assertThat(regulatoryDam.getSecurityLevel()).isEqualTo(securityLevel);
        assertThat(securityLevel.getRegulatoryDams()).contains(regulatoryDam);
    }

    @Test
    @DisplayName("Should support adding and removing regulatory dams")
    void shouldSupportAddingAndRemovingRegulatoryDams() {

        SecurityLevelEntity securityLevel = new SecurityLevelEntity();
        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);

        securityLevel.getRegulatoryDams().add(regulatoryDam);

        assertThat(securityLevel.getRegulatoryDams()).hasSize(1);

        securityLevel.getRegulatoryDams().remove(regulatoryDam);

        assertThat(securityLevel.getRegulatoryDams()).isEmpty();
    }
}
