package com.geosegbar.unit.infra.deterministic_limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.deterministic_limit.services.DeterministicLimitRules;

@Tag("unit")
@DisplayName("DeterministicLimitRules")
class DeterministicLimitRulesTest {

    private static BigDecimal dec(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @Test
    @DisplayName("aceita qualquer combinação com ao menos um nível")
    void acceptsAnyCombinationWithAtLeastOneLevel() {
        String[][] combinations = {
            {"1", "2", "3"},
            {"1", "2", null},
            {null, "2", "3"},
            {"1", null, "3"},
            {"1", null, null},
            {null, "2", null},
            {null, null, "3"}
        };
        for (String[] c : combinations) {
            assertThatCode(() -> {
                DeterministicLimitRules.requireAtLeastOneLevel(dec(c[0]), dec(c[1]), dec(c[2]), "Cota");
                DeterministicLimitRules.requireAscendingOrder(dec(c[0]), dec(c[1]), dec(c[2]), "Cota");
            }).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("rejeita limite sem nenhum nível")
    void rejectsLimitWithoutLevels() {
        assertThatThrownBy(() -> DeterministicLimitRules.requireAtLeastOneLevel(null, null, null, "Cota"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("'Cota'")
                .hasMessageContaining("ao menos um nível");
    }

    @Test
    @DisplayName("rejeita atenção maior ou igual ao alerta")
    void rejectsAttentionNotBelowAlert() {
        assertThatThrownBy(() -> DeterministicLimitRules.requireAscendingOrder(dec("780.66"), dec("766.90"), null, "Cota"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Atenção deve ser menor que o de Alerta");
        assertThatThrownBy(() -> DeterministicLimitRules.requireAscendingOrder(dec("423.88"), dec("423.88"), dec("425.40"), "Cota"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("rejeita alerta maior ou igual à emergência")
    void rejectsAlertNotBelowEmergency() {
        assertThatThrownBy(() -> DeterministicLimitRules.requireAscendingOrder(null, dec("999"), dec("999"), "Cota"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Alerta deve ser menor que o de Emergência");
    }

    @Test
    @DisplayName("compara atenção com emergência quando o alerta não se aplica")
    void comparesAttentionWithEmergencyWhenAlertIsAbsent() {
        assertThatThrownBy(() -> DeterministicLimitRules.requireAscendingOrder(dec("10"), null, dec("10"), "Cota"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Atenção deve ser menor que o de Emergência");
    }

    @Test
    @DisplayName("aceita valores negativos em ordem")
    void acceptsNegativeValuesInOrder() {
        assertThatCode(() -> DeterministicLimitRules.requireAscendingOrder(dec("-5"), dec("-2.5"), dec("0"), "Recalque"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("compara níveis pelo valor numérico, ignorando a escala")
    void comparesLevelsByNumericValue() {
        DeterministicLimitEntity current = new DeterministicLimitEntity();
        current.setAttentionValue(new BigDecimal("760.90"));
        current.setAlertValue(null);
        current.setEmergencyValue(new BigDecimal("762.00"));

        assertThat(DeterministicLimitRules.sameLevels(current, dec("760.9"), null, dec("762"))).isTrue();
        assertThat(DeterministicLimitRules.sameLevels(current, dec("760.9"), dec("761"), dec("762"))).isFalse();
        assertThat(DeterministicLimitRules.sameLevels(current, null, null, dec("762"))).isFalse();
    }
}
