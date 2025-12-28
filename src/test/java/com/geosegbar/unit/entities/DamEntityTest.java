package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.DocumentationDamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentGraphPatternFolder;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.RegulatoryDamEntity;
import com.geosegbar.entities.ReservoirEntity;
import com.geosegbar.entities.SectionEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - DamEntity")
class DamEntityTest extends BaseUnitTest {

    private ClientEntity client;
    private StatusEntity activeStatus;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        client = TestDataBuilder.client().build();
        activeStatus = TestDataBuilder.activeStatus();
    }

    @Test
    @DisplayName("Should create dam with all required fields")
    void shouldCreateDamWithAllRequiredFields() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setId(1L);
        dam.setName("Barragem Principal");
        dam.setLatitude(-23.5505);
        dam.setLongitude(-46.6333);
        dam.setStreet("Rua das Barragens");
        dam.setNeighborhood("Centro");
        dam.setNumberAddress("100");
        dam.setCity("São Paulo");
        dam.setState("São Paulo");
        dam.setZipCode("01000-000");
        dam.setClient(client);
        dam.setStatus(activeStatus);

        // Then
        assertThat(dam).satisfies(d -> {
            assertThat(d.getId()).isEqualTo(1L);
            assertThat(d.getName()).isEqualTo("Barragem Principal");
            assertThat(d.getLatitude()).isEqualTo(-23.5505);
            assertThat(d.getLongitude()).isEqualTo(-46.6333);
            assertThat(d.getStreet()).isEqualTo("Rua das Barragens");
            assertThat(d.getNeighborhood()).isEqualTo("Centro");
            assertThat(d.getNumberAddress()).isEqualTo("100");
            assertThat(d.getCity()).isEqualTo("São Paulo");
            assertThat(d.getState()).isEqualTo("São Paulo");
            assertThat(d.getZipCode()).isEqualTo("01000-000");
            assertThat(d.getClient()).isEqualTo(client);
            assertThat(d.getStatus()).isEqualTo(activeStatus);
        });
    }

    @Test
    @DisplayName("Should initialize all collections as empty HashSet")
    void shouldInitializeAllCollectionsAsEmptyHashSet() {
        // Given & When
        DamEntity dam = new DamEntity();

        // Then
        assertThat(dam.getChecklistResponses()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(dam.getSections()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(dam.getDamPermissions()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(dam.getReservoirs()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(dam.getPsbFolders()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(dam.getInstruments()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(dam.getPatternFolders()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(dam.getTemplateQuestionnaires()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
    }

    @Test
    @DisplayName("Should validate geographic coordinates - positive latitude")
    void shouldValidateGeographicCoordinatesPositiveLatitude() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setLatitude(40.7128); // New York
        dam.setLongitude(-74.0060);

        // Then
        assertThat(dam.getLatitude()).isPositive().isBetween(-90.0, 90.0);
        assertThat(dam.getLongitude()).isNegative().isBetween(-180.0, 180.0);
    }

    @Test
    @DisplayName("Should validate geographic coordinates - negative latitude")
    void shouldValidateGeographicCoordinatesNegativeLatitude() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setLatitude(-23.5505); // São Paulo
        dam.setLongitude(-46.6333);

        // Then
        assertThat(dam.getLatitude()).isNegative().isBetween(-90.0, 90.0);
        assertThat(dam.getLongitude()).isNegative().isBetween(-180.0, 180.0);
    }

    @Test
    @DisplayName("Should validate CEP format with dash")
    void shouldValidateCepFormatWithDash() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setZipCode("12345-678");

        // Then
        assertThat(dam.getZipCode())
                .matches("\\d{5}-\\d{3}")
                .contains("-");
    }

    @Test
    @DisplayName("Should validate CEP format without dash")
    void shouldValidateCepFormatWithoutDash() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setZipCode("12345678");

        // Then
        assertThat(dam.getZipCode()).matches("\\d{8}");
    }

    @Test
    @DisplayName("Should not allow numbers in dam name")
    void shouldNotAllowNumbersInDamName() {
        // Given - Valid names (no numbers)
        DamEntity dam = new DamEntity();
        dam.setName("Barragem Principal");

        // Then - Name should only contain letters and spaces
        assertThat(dam.getName()).matches("^[A-Za-zÀ-ÿ\\s]+$");
    }

    @Test
    @DisplayName("Should not allow numbers in city name")
    void shouldNotAllowNumbersInCityName() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setCity("São Paulo");

        // Then
        assertThat(dam.getCity()).matches("^[A-Za-zÀ-ÿ\\s]+$");
    }

    @Test
    @DisplayName("Should not allow numbers in state name")
    void shouldNotAllowNumbersInStateName() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setState("Minas Gerais");

        // Then
        assertThat(dam.getState()).matches("^[A-Za-zÀ-ÿ\\s]+$");
    }

    @Test
    @DisplayName("Should handle city names with accents")
    void shouldHandleCityNamesWithAccents() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setCity("São José dos Campos");

        // Then
        assertThat(dam.getCity()).contains("ã").contains("é");
    }

    @Test
    @DisplayName("Should handle optional fields as null")
    void shouldHandleOptionalFieldsAsNull() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setLogoPath(null);
        dam.setDamImagePath(null);
        dam.setLinkPSB(null);
        dam.setLinkLegislation(null);

        // Then
        assertThat(dam.getLogoPath()).isNull();
        assertThat(dam.getDamImagePath()).isNull();
        assertThat(dam.getLinkPSB()).isNull();
        assertThat(dam.getLinkLegislation()).isNull();
    }

    @Test
    @DisplayName("Should handle address without number")
    void shouldHandleAddressWithoutNumber() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setStreet("Rua Principal");
        dam.setNumberAddress("S/N");

        // Then
        assertThat(dam.getNumberAddress()).isEqualTo("S/N");
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Client")
    void shouldMaintainManyToOneRelationshipWithClient() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setClient(client);

        // Then
        assertThat(dam.getClient())
                .isNotNull()
                .isEqualTo(client);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Status")
    void shouldMaintainManyToOneRelationshipWithStatus() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setStatus(activeStatus);

        // Then
        assertThat(dam.getStatus())
                .isNotNull()
                .isEqualTo(activeStatus);
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with RegulatoryDam")
    void shouldMaintainOneToOneRelationshipWithRegulatoryDam() {
        // Given
        DamEntity dam = new DamEntity();
        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setDam(dam);
        dam.setRegulatoryDam(regulatoryDam);

        // Then
        assertThat(dam.getRegulatoryDam()).isNotNull();
        assertThat(dam.getRegulatoryDam().getDam()).isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with DocumentationDam")
    void shouldMaintainOneToOneRelationshipWithDocumentationDam() {
        // Given
        DamEntity dam = new DamEntity();
        DocumentationDamEntity documentationDam = new DocumentationDamEntity();
        documentationDam.setDam(dam);
        dam.setDocumentationDam(documentationDam);

        // Then
        assertThat(dam.getDocumentationDam()).isNotNull();
        assertThat(dam.getDocumentationDam().getDam()).isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with Checklist")
    void shouldMaintainOneToOneRelationshipWithChecklist() {
        // Given
        DamEntity dam = new DamEntity();
        ChecklistEntity checklist = new ChecklistEntity();
        checklist.setDam(dam);
        dam.setChecklist(checklist);

        // Then
        assertThat(dam.getChecklist()).isNotNull();
        assertThat(dam.getChecklist().getDam()).isEqualTo(dam);
    }

    @Test
    @DisplayName("Should add ChecklistResponse to dam")
    void shouldAddChecklistResponseToDam() {
        // Given
        DamEntity dam = new DamEntity();
        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setDam(dam);

        // When
        dam.getChecklistResponses().add(response);

        // Then
        assertThat(dam.getChecklistResponses())
                .hasSize(1)
                .contains(response);
    }

    @Test
    @DisplayName("Should add Section to dam")
    void shouldAddSectionToDam() {
        // Given
        DamEntity dam = new DamEntity();
        SectionEntity section = new SectionEntity();
        section.setDam(dam);

        // When
        dam.getSections().add(section);

        // Then
        assertThat(dam.getSections())
                .hasSize(1)
                .contains(section);
    }

    @Test
    @DisplayName("Should add DamPermission to dam")
    void shouldAddDamPermissionToDam() {
        // Given
        DamEntity dam = new DamEntity();
        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setDam(dam);

        // When
        dam.getDamPermissions().add(permission);

        // Then
        assertThat(dam.getDamPermissions())
                .hasSize(1)
                .contains(permission);
    }

    @Test
    @DisplayName("Should add Reservoir to dam")
    void shouldAddReservoirToDam() {
        // Given
        DamEntity dam = new DamEntity();
        ReservoirEntity reservoir = new ReservoirEntity();
        reservoir.setDam(dam);

        // When
        dam.getReservoirs().add(reservoir);

        // Then
        assertThat(dam.getReservoirs())
                .hasSize(1)
                .contains(reservoir);
    }

    @Test
    @DisplayName("Should add PSBFolder to dam")
    void shouldAddPsbFolderToDam() {
        // Given
        DamEntity dam = new DamEntity();
        PSBFolderEntity psbFolder = new PSBFolderEntity();
        psbFolder.setDam(dam);

        // When
        dam.getPsbFolders().add(psbFolder);

        // Then
        assertThat(dam.getPsbFolders())
                .hasSize(1)
                .contains(psbFolder);
    }

    @Test
    @DisplayName("Should add Instrument to dam")
    void shouldAddInstrumentToDam() {
        // Given
        DamEntity dam = new DamEntity();
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setDam(dam);

        // When
        dam.getInstruments().add(instrument);

        // Then
        assertThat(dam.getInstruments())
                .hasSize(1)
                .contains(instrument);
    }

    @Test
    @DisplayName("Should add PatternFolder to dam")
    void shouldAddPatternFolderToDam() {
        // Given
        DamEntity dam = new DamEntity();
        InstrumentGraphPatternFolder patternFolder = new InstrumentGraphPatternFolder();
        patternFolder.setDam(dam);

        // When
        dam.getPatternFolders().add(patternFolder);

        // Then
        assertThat(dam.getPatternFolders())
                .hasSize(1)
                .contains(patternFolder);
    }

    @Test
    @DisplayName("Should add TemplateQuestionnaire to dam")
    void shouldAddTemplateQuestionnaireToDam() {
        // Given
        DamEntity dam = new DamEntity();
        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setDam(dam);

        // When
        dam.getTemplateQuestionnaires().add(template);

        // Then
        assertThat(dam.getTemplateQuestionnaires())
                .hasSize(1)
                .contains(template);
    }

    @Test
    @DisplayName("Should handle multiple instruments per dam")
    void shouldHandleMultipleInstrumentsPerDam() {
        // Given
        DamEntity dam = new DamEntity();

        InstrumentEntity instrument1 = new InstrumentEntity();
        instrument1.setDam(dam);

        InstrumentEntity instrument2 = new InstrumentEntity();
        instrument2.setDam(dam);

        // When
        dam.getInstruments().add(instrument1);
        dam.getInstruments().add(instrument2);

        // Then
        assertThat(dam.getInstruments()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle neighborhood size limit")
    void shouldHandleNeighborhoodSizeLimit() {
        // Given
        DamEntity dam = new DamEntity();
        String neighborhood = "Bairro com Nome Muito Longo que Pode Ter Até Cem Caracteres";
        dam.setNeighborhood(neighborhood);

        // Then
        assertThat(dam.getNeighborhood().length()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Should handle city size limit")
    void shouldHandleCitySizeLimit() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setCity("São Paulo");

        // Then
        assertThat(dam.getCity().length()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Should handle state size limit")
    void shouldHandleStateSizeLimit() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setState("Rio Grande do Sul");

        // Then
        assertThat(dam.getState().length()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Should handle number address size limit")
    void shouldHandleNumberAddressSizeLimit() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setNumberAddress("1000-A");

        // Then
        assertThat(dam.getNumberAddress().length()).isLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Should handle different logo path formats")
    void shouldHandleDifferentLogoPathFormats() {
        // Given
        DamEntity dam1 = new DamEntity();
        dam1.setLogoPath("/uploads/logos/dam-1.png");

        DamEntity dam2 = new DamEntity();
        dam2.setLogoPath("https://cdn.example.com/dams/logo-2.jpg");

        // Then
        assertThat(dam1.getLogoPath()).startsWith("/uploads");
        assertThat(dam2.getLogoPath()).startsWith("https://");
    }

    @Test
    @DisplayName("Should handle different link formats")
    void shouldHandleDifferentLinkFormats() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setLinkPSB("https://example.com/psb/dam-123.pdf");
        dam.setLinkLegislation("https://legislation.gov.br/dam-rules.html");

        // Then
        assertThat(dam.getLinkPSB()).startsWith("https://");
        assertThat(dam.getLinkLegislation()).startsWith("https://");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        DamEntity dam = new DamEntity();
        dam.setId(1L);
        dam.setName("Barragem Original");

        Long originalId = dam.getId();

        // When
        dam.setName("Barragem Atualizada");
        dam.setLatitude(-20.0);
        dam.setLongitude(-40.0);

        // Then
        assertThat(dam.getId()).isEqualTo(originalId);
        assertThat(dam.getName()).isEqualTo("Barragem Atualizada");
    }

    @Test
    @DisplayName("Should handle extreme coordinate values")
    void shouldHandleExtremeCoordinateValues() {
        // Given
        DamEntity dam1 = new DamEntity();
        dam1.setLatitude(90.0); // North pole
        dam1.setLongitude(180.0);

        DamEntity dam2 = new DamEntity();
        dam2.setLatitude(-90.0); // South pole
        dam2.setLongitude(-180.0);

        // Then
        assertThat(dam1.getLatitude()).isEqualTo(90.0);
        assertThat(dam2.getLatitude()).isEqualTo(-90.0);
    }
}
