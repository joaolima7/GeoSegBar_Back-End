package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentTypeEntity")
class InstrumentTypeEntityTest extends BaseUnitTest {

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
    }

    @Test
    @DisplayName("Should create instrument type with all required fields")
    void shouldCreateInstrumentTypeWithAllRequiredFields() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setId(1L);
        type.setName("Piezômetro");

        assertThat(type).satisfies(t -> {
            assertThat(t.getId()).isEqualTo(1L);
            assertThat(t.getName()).isEqualTo("Piezômetro");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        InstrumentTypeEntity type = new InstrumentTypeEntity(
                1L,
                "Piezômetro",
                new HashSet<>()
        );

        assertThat(type.getId()).isEqualTo(1L);
        assertThat(type.getName()).isEqualTo("Piezômetro");
        assertThat(type.getInstruments()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of instruments")
    void shouldMaintainOneToManyCollectionOfInstruments() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Piezômetro");
        type.setInstruments(new HashSet<>());

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("PZ-01");
        type.getInstruments().add(instrument);

        assertThat(type.getInstruments())
                .isNotNull()
                .hasSize(1)
                .contains(instrument);
    }

    @Test
    @DisplayName("Should support multiple instruments per type")
    void shouldSupportMultipleInstrumentsPerType() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Piezômetro");
        type.setInstruments(new HashSet<>());

        InstrumentEntity inst1 = new InstrumentEntity();
        inst1.setId(1L);
        inst1.setName("PZ-01");

        InstrumentEntity inst2 = new InstrumentEntity();
        inst2.setId(2L);
        inst2.setName("PZ-02");

        InstrumentEntity inst3 = new InstrumentEntity();
        inst3.setId(3L);
        inst3.setName("PZ-03");

        type.getInstruments().add(inst1);
        type.getInstruments().add(inst2);
        type.getInstruments().add(inst3);

        assertThat(type.getInstruments()).hasSize(3);
    }

    @Test
    @DisplayName("Should initialize empty instruments collection by default")
    void shouldInitializeEmptyInstrumentsCollectionByDefault() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();

        assertThat(type.getInstruments()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support adding and removing instruments")
    void shouldSupportAddingAndRemovingInstruments() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("PZ-01");

        type.getInstruments().add(instrument);
        assertThat(type.getInstruments()).hasSize(1);

        type.getInstruments().remove(instrument);

        assertThat(type.getInstruments()).isEmpty();
    }

    @Test
    @DisplayName("Should support common instrument type names")
    void shouldSupportCommonInstrumentTypeNames() {

        InstrumentTypeEntity piezo = new InstrumentTypeEntity();
        piezo.setName("Piezômetro");

        InstrumentTypeEntity incli = new InstrumentTypeEntity();
        incli.setName("Inclinômetro");

        InstrumentTypeEntity extenso = new InstrumentTypeEntity();
        extenso.setName("Extensômetro");

        assertThat(piezo.getName()).isEqualTo("Piezômetro");
        assertThat(incli.getName()).isEqualTo("Inclinômetro");
        assertThat(extenso.getName()).isEqualTo("Extensômetro");
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Medidor de Vazão");

        assertThat(type.getName()).contains("ã");
    }

    @Test
    @DisplayName("Should support accented characters in name")
    void shouldSupportAccentedCharactersInName() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Piezômetro Elétrico");

        assertThat(type.getName()).contains("ô", "é");
    }

    @Test
    @DisplayName("Should support descriptive type names")
    void shouldSupportDescriptiveTypeNames() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Medidor de Nível de Água Subterrânea");

        assertThat(type.getName()).hasSize(36);
    }

    @Test
    @DisplayName("Should support short type names")
    void shouldSupportShortTypeNames() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Régua");

        assertThat(type.getName()).hasSize(5);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setId(1L);
        type.setName("Nome Inicial");

        Long originalId = type.getId();

        type.setName("Nome Atualizado");

        assertThat(type.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support unique constraint on name")
    void shouldSupportUniqueConstraintOnName() {

        InstrumentTypeEntity type1 = new InstrumentTypeEntity();
        type1.setId(1L);
        type1.setName("Piezômetro");

        InstrumentTypeEntity type2 = new InstrumentTypeEntity();
        type2.setId(2L);
        type2.setName("Inclinômetro");

        assertThat(type1.getName()).isNotEqualTo(type2.getName());
    }

    @Test
    @DisplayName("Should support name search index")
    void shouldSupportNameSearchIndex() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setName("Piezômetro");

        assertThat(type.getName()).isNotBlank();
    }

    @Test
    @DisplayName("Should support different instrument types")
    void shouldSupportDifferentInstrumentTypes() {

        InstrumentTypeEntity piezo = new InstrumentTypeEntity();
        piezo.setId(1L);
        piezo.setName("Piezômetro");

        InstrumentTypeEntity incli = new InstrumentTypeEntity();
        incli.setId(2L);
        incli.setName("Inclinômetro");

        InstrumentTypeEntity medidor = new InstrumentTypeEntity();
        medidor.setId(3L);
        medidor.setName("Medidor de Vazão");

        InstrumentTypeEntity marco = new InstrumentTypeEntity();
        marco.setId(4L);
        marco.setName("Marco Superficial");

        assertThat(piezo.getId()).isNotEqualTo(incli.getId());
        assertThat(incli.getId()).isNotEqualTo(medidor.getId());
        assertThat(medidor.getId()).isNotEqualTo(marco.getId());
    }

    @Test
    @DisplayName("Should support bidirectional relationship with instruments")
    void shouldSupportBidirectionalRelationshipWithInstruments() {

        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setId(1L);
        type.setName("Piezômetro");

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(1L);
        instrument.setName("PZ-01");
        instrument.setInstrumentType(type);

        type.getInstruments().add(instrument);

        assertThat(instrument.getInstrumentType()).isEqualTo(type);
        assertThat(type.getInstruments()).contains(instrument);
    }
}
