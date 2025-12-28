package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClassificationDamEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.PotentialDamageEntity;
import com.geosegbar.entities.RegulatoryDamEntity;
import com.geosegbar.entities.RiskCategoryEntity;
import com.geosegbar.entities.SecurityLevelEntity;

@Tag("unit")
class RegulatoryDamEntityTest extends BaseUnitTest {

    private DamEntity dam;
    private SecurityLevelEntity securityLevel;
    private RiskCategoryEntity riskCategory;
    private PotentialDamageEntity potentialDamage;
    private ClassificationDamEntity classificationDam;

    @BeforeEach
    void setUp() {
        dam = new DamEntity();
        dam.setId(1L);
        dam.setName("Barragem Principal");

        securityLevel = new SecurityLevelEntity();
        securityLevel.setId(1L);
        securityLevel.setLevel("Alto");

        riskCategory = new RiskCategoryEntity();
        riskCategory.setId(1L);
        riskCategory.setName("Alto Risco");

        potentialDamage = new PotentialDamageEntity();
        potentialDamage.setId(1L);
        potentialDamage.setName("Alto Dano");

        classificationDam = new ClassificationDamEntity();
        classificationDam.setId(1L);
        classificationDam.setClassification("Classe A");
    }

    @Test
    @DisplayName("Should create regulatory dam with all required fields")
    void shouldCreateRegulatoryDamWithAllRequiredFields() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);
        regulatoryDam.setDam(dam);
        regulatoryDam.setFramePNSB(true);

        assertThat(regulatoryDam).satisfies(rd -> {
            assertThat(rd.getId()).isEqualTo(1L);
            assertThat(rd.getDam()).isEqualTo(dam);
            assertThat(rd.getFramePNSB()).isTrue();
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity(
                1L,
                dam,
                true,
                "João Silva",
                "joao.silva@email.com",
                "11987654321",
                "Maria Santos",
                "maria.santos@email.com",
                "11976543210",
                securityLevel,
                "ANA - Agência Nacional de Águas",
                riskCategory,
                potentialDamage,
                classificationDam
        );

        assertThat(regulatoryDam.getId()).isEqualTo(1L);
        assertThat(regulatoryDam.getDam()).isEqualTo(dam);
        assertThat(regulatoryDam.getFramePNSB()).isTrue();
        assertThat(regulatoryDam.getRepresentativeName()).isEqualTo("João Silva");
        assertThat(regulatoryDam.getRepresentativeEmail()).isEqualTo("joao.silva@email.com");
        assertThat(regulatoryDam.getRepresentativePhone()).isEqualTo("11987654321");
        assertThat(regulatoryDam.getTechnicalManagerName()).isEqualTo("Maria Santos");
        assertThat(regulatoryDam.getTechnicalManagerEmail()).isEqualTo("maria.santos@email.com");
        assertThat(regulatoryDam.getTechnicalManagerPhone()).isEqualTo("11976543210");
        assertThat(regulatoryDam.getSecurityLevel()).isEqualTo(securityLevel);
        assertThat(regulatoryDam.getSupervisoryBodyName()).isEqualTo("ANA - Agência Nacional de Águas");
        assertThat(regulatoryDam.getRiskCategory()).isEqualTo(riskCategory);
        assertThat(regulatoryDam.getPotentialDamage()).isEqualTo(potentialDamage);
        assertThat(regulatoryDam.getClassificationDam()).isEqualTo(classificationDam);
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with Dam")
    void shouldMaintainOneToOneRelationshipWithDam() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setDam(dam);

        assertThat(regulatoryDam.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should support Boolean framePNSB true")
    void shouldSupportBooleanFramePNSBTrue() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setFramePNSB(true);

        assertThat(regulatoryDam.getFramePNSB()).isTrue();
    }

    @Test
    @DisplayName("Should support Boolean framePNSB false")
    void shouldSupportBooleanFramePNSBFalse() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setFramePNSB(false);

        assertThat(regulatoryDam.getFramePNSB()).isFalse();
    }

    @Test
    @DisplayName("Should support optional representative name")
    void shouldSupportOptionalRepresentativeName() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setRepresentativeName("Carlos Eduardo Ferreira");

        assertThat(regulatoryDam.getRepresentativeName()).isEqualTo("Carlos Eduardo Ferreira");
    }

    @Test
    @DisplayName("Should allow null representative name")
    void shouldAllowNullRepresentativeName() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setRepresentativeName(null);

        assertThat(regulatoryDam.getRepresentativeName()).isNull();
    }

    @Test
    @DisplayName("Should support valid representative email")
    void shouldSupportValidRepresentativeEmail() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setRepresentativeEmail("representante@geosegbar.com");

        assertThat(regulatoryDam.getRepresentativeEmail()).isEqualTo("representante@geosegbar.com");
    }

    @Test
    @DisplayName("Should allow null representative email")
    void shouldAllowNullRepresentativeEmail() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setRepresentativeEmail(null);

        assertThat(regulatoryDam.getRepresentativeEmail()).isNull();
    }

    @Test
    @DisplayName("Should support representative phone with 10 digits")
    void shouldSupportRepresentativePhoneWith10Digits() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setRepresentativePhone("1133334444");

        assertThat(regulatoryDam.getRepresentativePhone()).isEqualTo("1133334444");
    }

    @Test
    @DisplayName("Should support representative phone with 11 digits")
    void shouldSupportRepresentativePhoneWith11Digits() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setRepresentativePhone("11987654321");

        assertThat(regulatoryDam.getRepresentativePhone()).isEqualTo("11987654321");
    }

    @Test
    @DisplayName("Should allow null representative phone")
    void shouldAllowNullRepresentativePhone() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setRepresentativePhone(null);

        assertThat(regulatoryDam.getRepresentativePhone()).isNull();
    }

    @Test
    @DisplayName("Should support optional technical manager name")
    void shouldSupportOptionalTechnicalManagerName() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setTechnicalManagerName("Engenheiro Responsável Silva");

        assertThat(regulatoryDam.getTechnicalManagerName()).isEqualTo("Engenheiro Responsável Silva");
    }

    @Test
    @DisplayName("Should allow null technical manager name")
    void shouldAllowNullTechnicalManagerName() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setTechnicalManagerName(null);

        assertThat(regulatoryDam.getTechnicalManagerName()).isNull();
    }

    @Test
    @DisplayName("Should support valid technical manager email")
    void shouldSupportValidTechnicalManagerEmail() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setTechnicalManagerEmail("engenheiro.responsavel@geosegbar.com");

        assertThat(regulatoryDam.getTechnicalManagerEmail()).isEqualTo("engenheiro.responsavel@geosegbar.com");
    }

    @Test
    @DisplayName("Should allow null technical manager email")
    void shouldAllowNullTechnicalManagerEmail() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setTechnicalManagerEmail(null);

        assertThat(regulatoryDam.getTechnicalManagerEmail()).isNull();
    }

    @Test
    @DisplayName("Should support technical manager phone with 10 digits")
    void shouldSupportTechnicalManagerPhoneWith10Digits() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setTechnicalManagerPhone("2133334444");

        assertThat(regulatoryDam.getTechnicalManagerPhone()).isEqualTo("2133334444");
    }

    @Test
    @DisplayName("Should support technical manager phone with 11 digits")
    void shouldSupportTechnicalManagerPhoneWith11Digits() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setTechnicalManagerPhone("21987654321");

        assertThat(regulatoryDam.getTechnicalManagerPhone()).isEqualTo("21987654321");
    }

    @Test
    @DisplayName("Should allow null technical manager phone")
    void shouldAllowNullTechnicalManagerPhone() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setTechnicalManagerPhone(null);

        assertThat(regulatoryDam.getTechnicalManagerPhone()).isNull();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with SecurityLevel")
    void shouldMaintainManyToOneRelationshipWithSecurityLevel() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setSecurityLevel(securityLevel);

        assertThat(regulatoryDam.getSecurityLevel())
                .isNotNull()
                .isEqualTo(securityLevel);
    }

    @Test
    @DisplayName("Should allow null security level")
    void shouldAllowNullSecurityLevel() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setSecurityLevel(null);

        assertThat(regulatoryDam.getSecurityLevel()).isNull();
    }

    @Test
    @DisplayName("Should support optional supervisory body name")
    void shouldSupportOptionalSupervisoryBodyName() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setSupervisoryBodyName("INEMA - Instituto do Meio Ambiente");

        assertThat(regulatoryDam.getSupervisoryBodyName()).isEqualTo("INEMA - Instituto do Meio Ambiente");
    }

    @Test
    @DisplayName("Should allow null supervisory body name")
    void shouldAllowNullSupervisoryBodyName() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setSupervisoryBodyName(null);

        assertThat(regulatoryDam.getSupervisoryBodyName()).isNull();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with RiskCategory")
    void shouldMaintainManyToOneRelationshipWithRiskCategory() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setRiskCategory(riskCategory);

        assertThat(regulatoryDam.getRiskCategory())
                .isNotNull()
                .isEqualTo(riskCategory);
    }

    @Test
    @DisplayName("Should allow null risk category")
    void shouldAllowNullRiskCategory() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setRiskCategory(null);

        assertThat(regulatoryDam.getRiskCategory()).isNull();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with PotentialDamage")
    void shouldMaintainManyToOneRelationshipWithPotentialDamage() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setPotentialDamage(potentialDamage);

        assertThat(regulatoryDam.getPotentialDamage())
                .isNotNull()
                .isEqualTo(potentialDamage);
    }

    @Test
    @DisplayName("Should allow null potential damage")
    void shouldAllowNullPotentialDamage() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setPotentialDamage(null);

        assertThat(regulatoryDam.getPotentialDamage()).isNull();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with ClassificationDam")
    void shouldMaintainManyToOneRelationshipWithClassificationDam() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setClassificationDam(classificationDam);

        assertThat(regulatoryDam.getClassificationDam())
                .isNotNull()
                .isEqualTo(classificationDam);
    }

    @Test
    @DisplayName("Should allow null classification dam")
    void shouldAllowNullClassificationDam() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setClassificationDam(null);

        assertThat(regulatoryDam.getClassificationDam()).isNull();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setId(1L);
        regulatoryDam.setDam(dam);
        regulatoryDam.setFramePNSB(true);

        Long originalId = regulatoryDam.getId();

        regulatoryDam.setFramePNSB(false);
        regulatoryDam.setRepresentativeName("Novo Representante");

        assertThat(regulatoryDam.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support complete regulatory information")
    void shouldSupportCompleteRegulatoryInformation() {

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setDam(dam);
        regulatoryDam.setFramePNSB(true);
        regulatoryDam.setRepresentativeName("João Silva");
        regulatoryDam.setRepresentativeEmail("joao@email.com");
        regulatoryDam.setRepresentativePhone("11987654321");
        regulatoryDam.setTechnicalManagerName("Maria Santos");
        regulatoryDam.setTechnicalManagerEmail("maria@email.com");
        regulatoryDam.setTechnicalManagerPhone("11976543210");
        regulatoryDam.setSecurityLevel(securityLevel);
        regulatoryDam.setSupervisoryBodyName("ANA");
        regulatoryDam.setRiskCategory(riskCategory);
        regulatoryDam.setPotentialDamage(potentialDamage);
        regulatoryDam.setClassificationDam(classificationDam);

        assertThat(regulatoryDam.getDam()).isEqualTo(dam);
        assertThat(regulatoryDam.getFramePNSB()).isTrue();
        assertThat(regulatoryDam.getRepresentativeName()).isEqualTo("João Silva");
        assertThat(regulatoryDam.getSecurityLevel()).isEqualTo(securityLevel);
        assertThat(regulatoryDam.getRiskCategory()).isEqualTo(riskCategory);
        assertThat(regulatoryDam.getPotentialDamage()).isEqualTo(potentialDamage);
        assertThat(regulatoryDam.getClassificationDam()).isEqualTo(classificationDam);
    }
}
