package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.RegulatoryDamEntity;
import com.geosegbar.entities.RiskCategoryEntity;

@Tag("unit")
class RiskCategoryEntityTest extends BaseUnitTest {

    @Test
    @DisplayName("Should create risk category with all required fields")
    void shouldCreateRiskCategoryWithAllRequiredFields() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();
        riskCategory.setId(1L);
        riskCategory.setName("Alto Risco");

        assertThat(riskCategory).satisfies(rc -> {
            assertThat(rc.getId()).isEqualTo(1L);
            assertThat(rc.getName()).isEqualTo("Alto Risco");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity(
                1L,
                "Médio Risco",
                new HashSet<>()
        );

        assertThat(riskCategory.getId()).isEqualTo(1L);
        assertThat(riskCategory.getName()).isEqualTo("Médio Risco");
        assertThat(riskCategory.getRegulatoryDams()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of regulatory dams")
    void shouldMaintainOneToManyCollectionOfRegulatoryDams() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();
        riskCategory.setName("Alto Risco");

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);
        regulatoryDam.setRiskCategory(riskCategory);

        riskCategory.getRegulatoryDams().add(regulatoryDam);

        assertThat(riskCategory.getRegulatoryDams())
                .isNotNull()
                .hasSize(1)
                .contains(regulatoryDam);
    }

    @Test
    @DisplayName("Should initialize empty regulatory dams collection by default")
    void shouldInitializeEmptyRegulatoryDamsCollectionByDefault() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();

        assertThat(riskCategory.getRegulatoryDams()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing regulatory dams")
    void shouldSupportAddingAndRemovingRegulatoryDams() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();
        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);

        riskCategory.getRegulatoryDams().add(regulatoryDam);
        assertThat(riskCategory.getRegulatoryDams()).hasSize(1);

        riskCategory.getRegulatoryDams().remove(regulatoryDam);

        assertThat(riskCategory.getRegulatoryDams()).isEmpty();
    }

    @Test
    @DisplayName("Should support multiple regulatory dams per risk category")
    void shouldSupportMultipleRegulatoryDamsPerRiskCategory() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();
        riskCategory.setName("Alto Risco");

        RegulatoryDamEntity dam1 = new RegulatoryDamEntity();
        dam1.setId(1L);

        RegulatoryDamEntity dam2 = new RegulatoryDamEntity();
        dam2.setId(2L);

        RegulatoryDamEntity dam3 = new RegulatoryDamEntity();
        dam3.setId(3L);

        riskCategory.getRegulatoryDams().add(dam1);
        riskCategory.getRegulatoryDams().add(dam2);
        riskCategory.getRegulatoryDams().add(dam3);

        assertThat(riskCategory.getRegulatoryDams()).hasSize(3);
    }

    @Test
    @DisplayName("Should support common risk category names")
    void shouldSupportCommonRiskCategoryNames() {

        RiskCategoryEntity baixo = new RiskCategoryEntity();
        baixo.setName("Baixo");

        RiskCategoryEntity medio = new RiskCategoryEntity();
        medio.setName("Médio");

        RiskCategoryEntity alto = new RiskCategoryEntity();
        alto.setName("Alto");

        assertThat(baixo.getName()).isEqualTo("Baixo");
        assertThat(medio.getName()).isEqualTo("Médio");
        assertThat(alto.getName()).isEqualTo("Alto");
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();
        riskCategory.setName("Categoria de Risco Médio com Atenção");

        assertThat(riskCategory.getName()).contains("é", "ç", "ã");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();
        riskCategory.setId(1L);
        riskCategory.setName("Alto Risco");

        Long originalId = riskCategory.getId();

        riskCategory.setName("Altíssimo Risco");

        assertThat(riskCategory.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support descriptive risk category names")
    void shouldSupportDescriptiveRiskCategoryNames() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();
        riskCategory.setName("Alto Risco - Requer Monitoramento Constante");

        assertThat(riskCategory.getName()).hasSize(43);
    }

    @Test
    @DisplayName("Should support short risk category names")
    void shouldSupportShortRiskCategoryNames() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();
        riskCategory.setName("A");

        assertThat(riskCategory.getName()).hasSize(1);
    }

    @Test
    @DisplayName("Should support lazy fetch for regulatory dams")
    void shouldSupportLazyFetchForRegulatoryDams() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();
        riskCategory.setName("Alto Risco");

        assertThat(riskCategory.getRegulatoryDams()).isNotNull();
    }

    @Test
    @DisplayName("Should support bidirectional relationship with regulatory dams")
    void shouldSupportBidirectionalRelationshipWithRegulatoryDams() {

        RiskCategoryEntity riskCategory = new RiskCategoryEntity();
        riskCategory.setId(1L);
        riskCategory.setName("Alto Risco");

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);
        regulatoryDam.setRiskCategory(riskCategory);

        riskCategory.getRegulatoryDams().add(regulatoryDam);

        assertThat(riskCategory.getRegulatoryDams()).contains(regulatoryDam);
        assertThat(regulatoryDam.getRiskCategory()).isEqualTo(riskCategory);
    }

    @Test
    @DisplayName("Should support different risk classification levels")
    void shouldSupportDifferentRiskClassificationLevels() {

        RiskCategoryEntity baixo = new RiskCategoryEntity();
        baixo.setId(1L);
        baixo.setName("Baixo");

        RiskCategoryEntity medio = new RiskCategoryEntity();
        medio.setId(2L);
        medio.setName("Médio");

        RiskCategoryEntity alto = new RiskCategoryEntity();
        alto.setId(3L);
        alto.setName("Alto");

        RiskCategoryEntity muitoAlto = new RiskCategoryEntity();
        muitoAlto.setId(4L);
        muitoAlto.setName("Muito Alto");

        assertThat(baixo.getName()).isNotEqualTo(medio.getName());
        assertThat(medio.getName()).isNotEqualTo(alto.getName());
        assertThat(alto.getName()).isNotEqualTo(muitoAlto.getName());
    }

    @Test
    @DisplayName("Should support unique name constraint concept")
    void shouldSupportUniqueNameConstraintConcept() {

        RiskCategoryEntity riskCategory1 = new RiskCategoryEntity();
        riskCategory1.setId(1L);
        riskCategory1.setName("Alto Risco");

        RiskCategoryEntity riskCategory2 = new RiskCategoryEntity();
        riskCategory2.setId(2L);
        riskCategory2.setName("Médio Risco");

        assertThat(riskCategory1.getName()).isNotEqualTo(riskCategory2.getName());
    }
}
