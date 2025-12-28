package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.SectionEntity;

@Tag("unit")
class SectionEntityTest extends BaseUnitTest {

    private DamEntity dam;

    @BeforeEach
    void setUp() {
        dam = new DamEntity();
        dam.setId(1L);
        dam.setName("Barragem Principal");
    }

    @Test
    @DisplayName("Should create section with all required fields")
    void shouldCreateSectionWithAllRequiredFields() {

        SectionEntity section = new SectionEntity();
        section.setId(1L);
        section.setName("Seção A");
        section.setFirstVertexLatitude(-23.5505);
        section.setSecondVertexLatitude(-23.5515);
        section.setFirstVertexLongitude(-46.6333);
        section.setSecondVertexLongitude(-46.6343);

        assertThat(section).satisfies(s -> {
            assertThat(s.getId()).isEqualTo(1L);
            assertThat(s.getName()).isEqualTo("Seção A");
            assertThat(s.getFirstVertexLatitude()).isEqualTo(-23.5505);
            assertThat(s.getSecondVertexLatitude()).isEqualTo(-23.5515);
            assertThat(s.getFirstVertexLongitude()).isEqualTo(-46.6333);
            assertThat(s.getSecondVertexLongitude()).isEqualTo(-46.6343);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        SectionEntity section = new SectionEntity(
                1L,
                "Seção B",
                "/sections/section-b.json",
                -23.5505,
                -23.5515,
                -46.6333,
                -46.6343,
                dam,
                null
        );

        assertThat(section.getId()).isEqualTo(1L);
        assertThat(section.getName()).isEqualTo("Seção B");
        assertThat(section.getFilePath()).isEqualTo("/sections/section-b.json");
        assertThat(section.getFirstVertexLatitude()).isEqualTo(-23.5505);
        assertThat(section.getSecondVertexLatitude()).isEqualTo(-23.5515);
        assertThat(section.getFirstVertexLongitude()).isEqualTo(-46.6333);
        assertThat(section.getSecondVertexLongitude()).isEqualTo(-46.6343);
        assertThat(section.getDam()).isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Dam")
    void shouldMaintainManyToOneRelationshipWithDam() {

        SectionEntity section = new SectionEntity();
        section.setDam(dam);

        assertThat(section.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should allow null dam")
    void shouldAllowNullDam() {

        SectionEntity section = new SectionEntity();
        section.setName("Seção Sem Barragem");
        section.setFirstVertexLatitude(-23.5505);
        section.setSecondVertexLatitude(-23.5515);
        section.setFirstVertexLongitude(-46.6333);
        section.setSecondVertexLongitude(-46.6343);
        section.setDam(null);

        assertThat(section.getDam()).isNull();
    }

    @Test
    @DisplayName("Should support optional file path")
    void shouldSupportOptionalFilePath() {

        SectionEntity section = new SectionEntity();
        section.setFilePath("/data/sections/section-1.geojson");

        assertThat(section.getFilePath()).isEqualTo("/data/sections/section-1.geojson");
    }

    @Test
    @DisplayName("Should allow null file path")
    void shouldAllowNullFilePath() {

        SectionEntity section = new SectionEntity();
        section.setFilePath(null);

        assertThat(section.getFilePath()).isNull();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of instruments")
    void shouldMaintainOneToManyCollectionOfInstruments() {

        SectionEntity section = new SectionEntity();
        section.setName("Seção A");

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setSection(section);

        section.getInstruments().add(instrument);

        assertThat(section.getInstruments())
                .isNotNull()
                .hasSize(1)
                .contains(instrument);
    }

    @Test
    @DisplayName("Should initialize empty instruments collection by default")
    void shouldInitializeEmptyInstrumentsCollectionByDefault() {

        SectionEntity section = new SectionEntity();

        assertThat(section.getInstruments()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support multiple instruments per section")
    void shouldSupportMultipleInstrumentsPerSection() {

        SectionEntity section = new SectionEntity();

        InstrumentEntity instrument1 = new InstrumentEntity();
        instrument1.setId(1L);

        InstrumentEntity instrument2 = new InstrumentEntity();
        instrument2.setId(2L);

        InstrumentEntity instrument3 = new InstrumentEntity();
        instrument3.setId(3L);

        section.getInstruments().add(instrument1);
        section.getInstruments().add(instrument2);
        section.getInstruments().add(instrument3);

        assertThat(section.getInstruments()).hasSize(3);
    }

    @Test
    @DisplayName("Should support positive latitude coordinates")
    void shouldSupportPositiveLatitudeCoordinates() {

        SectionEntity section = new SectionEntity();
        section.setFirstVertexLatitude(40.7128);
        section.setSecondVertexLatitude(40.7138);

        assertThat(section.getFirstVertexLatitude()).isEqualTo(40.7128);
        assertThat(section.getSecondVertexLatitude()).isEqualTo(40.7138);
    }

    @Test
    @DisplayName("Should support negative latitude coordinates")
    void shouldSupportNegativeLatitudeCoordinates() {

        SectionEntity section = new SectionEntity();
        section.setFirstVertexLatitude(-23.5505);
        section.setSecondVertexLatitude(-23.5515);

        assertThat(section.getFirstVertexLatitude()).isEqualTo(-23.5505);
        assertThat(section.getSecondVertexLatitude()).isEqualTo(-23.5515);
    }

    @Test
    @DisplayName("Should support positive longitude coordinates")
    void shouldSupportPositiveLongitudeCoordinates() {

        SectionEntity section = new SectionEntity();
        section.setFirstVertexLongitude(139.6917);
        section.setSecondVertexLongitude(139.6927);

        assertThat(section.getFirstVertexLongitude()).isEqualTo(139.6917);
        assertThat(section.getSecondVertexLongitude()).isEqualTo(139.6927);
    }

    @Test
    @DisplayName("Should support negative longitude coordinates")
    void shouldSupportNegativeLongitudeCoordinates() {

        SectionEntity section = new SectionEntity();
        section.setFirstVertexLongitude(-46.6333);
        section.setSecondVertexLongitude(-46.6343);

        assertThat(section.getFirstVertexLongitude()).isEqualTo(-46.6333);
        assertThat(section.getSecondVertexLongitude()).isEqualTo(-46.6343);
    }

    @Test
    @DisplayName("Should support high precision coordinates")
    void shouldSupportHighPrecisionCoordinates() {

        SectionEntity section = new SectionEntity();
        section.setFirstVertexLatitude(-23.550520123);
        section.setSecondVertexLatitude(-23.551530456);
        section.setFirstVertexLongitude(-46.633340789);
        section.setSecondVertexLongitude(-46.634350012);

        assertThat(section.getFirstVertexLatitude()).isEqualTo(-23.550520123);
        assertThat(section.getSecondVertexLatitude()).isEqualTo(-23.551530456);
        assertThat(section.getFirstVertexLongitude()).isEqualTo(-46.633340789);
        assertThat(section.getSecondVertexLongitude()).isEqualTo(-46.634350012);
    }

    @Test
    @DisplayName("Should support descriptive section names")
    void shouldSupportDescriptiveSectionNames() {

        SectionEntity section = new SectionEntity();
        section.setName("Seção de Monitoramento Norte");

        assertThat(section.getName()).isEqualTo("Seção de Monitoramento Norte");
    }

    @Test
    @DisplayName("Should support short section names")
    void shouldSupportShortSectionNames() {

        SectionEntity section = new SectionEntity();
        section.setName("A");

        assertThat(section.getName()).hasSize(1);
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {

        SectionEntity section = new SectionEntity();
        section.setName("Seção de Instrumentação");

        assertThat(section.getName()).contains("ç", "ã");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        SectionEntity section = new SectionEntity();
        section.setId(1L);
        section.setName("Seção A");

        Long originalId = section.getId();

        section.setName("Seção B");
        section.setFirstVertexLatitude(-23.5505);

        assertThat(section.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support multiple sections per dam")
    void shouldSupportMultipleSectionsPerDam() {

        SectionEntity section1 = new SectionEntity();
        section1.setId(1L);
        section1.setName("Seção Norte");
        section1.setDam(dam);

        SectionEntity section2 = new SectionEntity();
        section2.setId(2L);
        section2.setName("Seção Sul");
        section2.setDam(dam);

        assertThat(section1.getDam()).isEqualTo(section2.getDam());
        assertThat(section1.getName()).isNotEqualTo(section2.getName());
    }

    @Test
    @DisplayName("Should support rectangular section coordinates")
    void shouldSupportRectangularSectionCoordinates() {

        SectionEntity section = new SectionEntity();
        section.setName("Seção Retangular");
        section.setFirstVertexLatitude(-23.5505);
        section.setSecondVertexLatitude(-23.5515);
        section.setFirstVertexLongitude(-46.6333);
        section.setSecondVertexLongitude(-46.6343);

        assertThat(section.getFirstVertexLatitude()).isNotEqualTo(section.getSecondVertexLatitude());
        assertThat(section.getFirstVertexLongitude()).isNotEqualTo(section.getSecondVertexLongitude());
    }
}
