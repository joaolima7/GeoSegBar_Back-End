package com.geosegbar.infra.deterministic_limit.services;

import java.math.BigDecimal;

import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.exceptions.InvalidInputException;

/**
 * Regras dos níveis de um limite determinístico. Cada nível (Atenção, Alerta,
 * Emergência) é opcional: valor nulo significa que o nível não se aplica ao
 * output. O limite precisa de ao menos um nível, e os níveis preenchidos seguem
 * a ordem Atenção < Alerta < Emergência.
 */
public final class DeterministicLimitRules {

    private DeterministicLimitRules() {
    }

    public static void requireAtLeastOneLevel(BigDecimal attention, BigDecimal alert, BigDecimal emergency, String outputName) {
        if (attention == null && alert == null && emergency == null) {
            throw new InvalidInputException(String.format(
                    "O limite determinístico do output '%s' precisa de ao menos um nível preenchido (Atenção, Alerta ou Emergência).",
                    outputName));
        }
    }

    public static void requireAscendingOrder(BigDecimal attention, BigDecimal alert, BigDecimal emergency, String outputName) {
        if (isNotBelow(attention, alert)) {
            throw orderError(outputName, "Atenção", "Alerta");
        }
        if (isNotBelow(alert, emergency)) {
            throw orderError(outputName, "Alerta", "Emergência");
        }
        if (isNotBelow(attention, emergency)) {
            throw orderError(outputName, "Atenção", "Emergência");
        }
    }

    /**
     * Compara os níveis por valor numérico: 760.9 e 760.90 são o mesmo nível,
     * embora {@link BigDecimal#equals} diga que não.
     */
    public static boolean sameLevels(DeterministicLimitEntity current, BigDecimal attention, BigDecimal alert, BigDecimal emergency) {
        return sameValue(current.getAttentionValue(), attention)
                && sameValue(current.getAlertValue(), alert)
                && sameValue(current.getEmergencyValue(), emergency);
    }

    public static boolean sameValue(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private static boolean isNotBelow(BigDecimal lower, BigDecimal upper) {
        return lower != null && upper != null && lower.compareTo(upper) >= 0;
    }

    private static InvalidInputException orderError(String outputName, String lowerLevel, String upperLevel) {
        return new InvalidInputException(String.format(
                "Limite determinístico do output '%s': o valor de %s deve ser menor que o de %s.",
                outputName, lowerLevel, upperLevel));
    }
}
