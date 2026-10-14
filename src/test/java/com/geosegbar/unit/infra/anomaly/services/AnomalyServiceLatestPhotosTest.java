package com.geosegbar.unit.infra.anomaly.services;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.infra.anomaly.dtos.AnomalyLatestPhotoDTO;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyLatestPhotoProjection;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.anomaly.services.AnomalyService;
import com.geosegbar.infra.dam.services.DamAccessService;

@DisplayName("AnomalyService — foto mais recente do ponto da anomalia")
class AnomalyServiceLatestPhotosTest extends BaseUnitTest {

    @Mock
    private AnomalyRepository anomalyRepository;
    @Mock
    private DamAccessService damAccessService;

    @InjectMocks
    private AnomalyService anomalyService;

    private static AnomalyLatestPhotoProjection row(Long anomalyId, Long responseId, LocalDateTime date, String path) {
        return new AnomalyLatestPhotoProjection() {
            @Override
            public Long getAnomalyId() {
                return anomalyId;
            }

            @Override
            public Long getChecklistResponseId() {
                return responseId;
            }

            @Override
            public LocalDateTime getPhotoDate() {
                return date;
            }

            @Override
            public String getImagePath() {
                return path;
            }
        };
    }

    @Test
    @DisplayName("agrupa as fotos da mesma resposta por anomalia, na ordem da consulta")
    void groupsPhotosByAnomaly() {
        LocalDateTime june = LocalDateTime.of(2026, 6, 30, 9, 0);
        LocalDateTime july = LocalDateTime.of(2026, 7, 31, 9, 0);
        when(anomalyRepository.findLatestPhotosByDamId(7L)).thenReturn(List.of(
                row(1L, 40L, july, "a.jpg"),
                row(1L, 40L, july, "b.jpg"),
                row(2L, 38L, june, "c.jpg")));

        List<AnomalyLatestPhotoDTO> result = anomalyService.findLatestPhotosByDamId(7L);

        assertThat(result).containsExactly(
                new AnomalyLatestPhotoDTO(1L, 40L, july, List.of("a.jpg", "b.jpg")),
                new AnomalyLatestPhotoDTO(2L, 38L, june, List.of("c.jpg")));
    }

    @Test
    @DisplayName("barra quem não tem acesso à barragem antes de consultar")
    void requiresDamAccess() {
        doThrow(new ForbiddenException("sem acesso")).when(damAccessService).requireAccess(7L);

        assertThatThrownBy(() -> anomalyService.findLatestPhotosByDamId(7L))
                .isInstanceOf(ForbiddenException.class);
        verify(anomalyRepository, never()).findLatestPhotosByDamId(7L);
    }
}
