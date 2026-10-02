package com.geosegbar.unit.infra.checklist_response.services;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyPhotoEntity;
import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.infra.checklist_response.dtos.ChecklistAnomalyDTO;
import com.geosegbar.infra.checklist_response.services.ChecklistAnomalyMapper;

@DisplayName("ChecklistAnomalyMapper")
class ChecklistAnomalyMapperTest extends BaseUnitTest {

    private static AnomalyEntity anomaly() {
        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setId(9L);
        anomaly.setObservation("Erosão na crista");
        anomaly.setRecommendation("Recompor o talude");
        anomaly.setLatitude(-23.5);
        anomaly.setLongitude(-46.6);
        anomaly.setCreatedAt(LocalDateTime.of(2026, 4, 15, 14, 0));
        return anomaly;
    }

    @Test
    @DisplayName("um apontamento de 'Outros' chega sem pergunta vinculada")
    void mapsOtherAnomalyWithoutQuestion() {
        AnomalyEntity anomaly = anomaly();
        anomaly.setQuestionId(null);
        anomaly.setQuestionnaireId(10L);

        ChecklistAnomalyDTO dto = ChecklistAnomalyMapper.toDto(anomaly);

        assertThat(dto.getId()).isEqualTo(9L);
        assertThat(dto.getQuestionId()).isNull();
        assertThat(dto.getQuestionnaireId()).isEqualTo(10L);
        assertThat(dto.getObservation()).isEqualTo("Erosão na crista");
        assertThat(dto.getRecommendation()).isEqualTo("Recompor o talude");
        assertThat(dto.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 15, 14, 0));
    }

    @Test
    @DisplayName("leva o nome do nível de perigo e do status, não só o id")
    void mapsDangerLevelAndStatusNames() {
        AnomalyEntity anomaly = anomaly();
        DangerLevelEntity dangerLevel = new DangerLevelEntity();
        dangerLevel.setId(2L);
        dangerLevel.setName("Atenção");
        AnomalyStatusEntity status = new AnomalyStatusEntity();
        status.setId(1L);
        status.setName("Pendente");
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);

        ChecklistAnomalyDTO dto = ChecklistAnomalyMapper.toDto(anomaly);

        assertThat(dto.getDangerLevelId()).isEqualTo(2L);
        assertThat(dto.getDangerLevelName()).isEqualTo("Atenção");
        assertThat(dto.getStatusId()).isEqualTo(1L);
        assertThat(dto.getStatusName()).isEqualTo("Pendente");
    }

    @Test
    @DisplayName("anomalia sem nível nem status não explode")
    void toleratesMissingClassification() {
        AnomalyEntity anomaly = anomaly();
        anomaly.setDangerLevel(null);
        anomaly.setStatus(null);

        ChecklistAnomalyDTO dto = ChecklistAnomalyMapper.toDto(anomaly);

        assertThat(dto.getDangerLevelId()).isNull();
        assertThat(dto.getDangerLevelName()).isNull();
        assertThat(dto.getStatusId()).isNull();
        assertThat(dto.getStatusName()).isNull();
    }

    @Test
    @DisplayName("leva as fotos em ordem de id")
    void mapsPhotoUrlsOrderedById() {
        AnomalyEntity anomaly = anomaly();
        LinkedHashSet<AnomalyPhotoEntity> photos = new LinkedHashSet<>();
        photos.add(photo(20L, "https://s3/b.jpg"));
        photos.add(photo(10L, "https://s3/a.jpg"));
        anomaly.setPhotos(photos);

        ChecklistAnomalyDTO dto = ChecklistAnomalyMapper.toDto(anomaly);

        assertThat(dto.getPhotoUrls()).containsExactly("https://s3/a.jpg", "https://s3/b.jpg");
    }

    @Test
    @DisplayName("anomalia sem foto vira lista vazia, nunca nulo")
    void mapsEmptyPhotosToEmptyList() {
        AnomalyEntity anomaly = anomaly();
        anomaly.setPhotos(new LinkedHashSet<>());

        ChecklistAnomalyDTO dto = ChecklistAnomalyMapper.toDto(anomaly);

        assertThat(dto.getPhotoUrls()).isEqualTo(List.of());
    }

    private static AnomalyPhotoEntity photo(Long id, String path) {
        AnomalyPhotoEntity photo = new AnomalyPhotoEntity();
        photo.setId(id);
        photo.setImagePath(path);
        return photo;
    }
}
