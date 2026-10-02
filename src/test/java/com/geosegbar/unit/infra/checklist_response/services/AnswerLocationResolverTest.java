package com.geosegbar.unit.infra.checklist_response.services;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.infra.checklist_response.dtos.AnswerUpdateDTO;
import com.geosegbar.infra.checklist_response.services.AnswerLocationResolver;

@DisplayName("AnswerLocationResolver")
class AnswerLocationResolverTest extends BaseUnitTest {

    private static AnswerEntity stored(Double latitude, Double longitude) {
        AnswerEntity answer = new AnswerEntity();
        answer.setLatitude(latitude);
        answer.setLongitude(longitude);
        return answer;
    }

    private static AnswerUpdateDTO sent(Double latitude, Double longitude) {
        AnswerUpdateDTO dto = new AnswerUpdateDTO();
        dto.setLatitude(latitude);
        dto.setLongitude(longitude);
        return dto;
    }

    @Test
    @DisplayName("a localização enviada vence a gravada")
    void sentLocationWins() {
        AnswerEntity answer = stored(-10.0, -20.0);

        assertThat(AnswerLocationResolver.resolveLatitude(answer, sent(-23.5, -46.6))).isEqualTo(-23.5);
        assertThat(AnswerLocationResolver.resolveLongitude(answer, sent(-23.5, -46.6))).isEqualTo(-46.6);
    }

    @Test
    @DisplayName("sem localização enviada, mantém a que já estava gravada")
    void keepsStoredWhenNothingSent() {
        AnswerEntity answer = stored(-10.0, -20.0);

        assertThat(AnswerLocationResolver.resolveLatitude(answer, sent(null, null))).isEqualTo(-10.0);
        assertThat(AnswerLocationResolver.resolveLongitude(answer, sent(null, null))).isEqualTo(-20.0);
    }

    @Test
    @DisplayName("resposta sem localização que recebe uma passa a ter localização")
    void answerWithoutLocationGainsOne() {
        AnswerEntity answer = stored(null, null);

        assertThat(AnswerLocationResolver.hasLocation(answer, sent(-23.5, -46.6))).isTrue();
    }

    @Test
    @DisplayName("resposta sem localização que não recebe nenhuma continua sem")
    void answerWithoutLocationStaysWithout() {
        AnswerEntity answer = stored(null, null);

        assertThat(AnswerLocationResolver.hasLocation(answer, sent(null, null))).isFalse();
    }

    @Test
    @DisplayName("latitude sem longitude não conta como localização")
    void halfLocationIsNoLocation() {
        AnswerEntity answer = stored(null, null);

        assertThat(AnswerLocationResolver.hasLocation(answer, sent(-23.5, null))).isFalse();
    }
}
