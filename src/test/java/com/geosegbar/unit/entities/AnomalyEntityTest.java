package com.geosegbar.unit.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.enums.AnomalyOriginEnum;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyPhotoEntity;
import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - AnomalyEntity")
class AnomalyEntityTest extends BaseUnitTest {

    private UserEntity user;
    private DamEntity dam;
    private DangerLevelEntity dangerLevel;
    private AnomalyStatusEntity status;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        user = TestDataBuilder.user().asAdmin().build();
        dam = TestDataBuilder.dam().withName("Barragem Principal").build();
        dangerLevel = new DangerLevelEntity(1L, "Alto", "Perigo alto");
        status = new AnomalyStatusEntity(1L, "Aberta", "Anomalia identificada");
    }

    @Test
    @DisplayName("Should create anomaly with all required fields")
    void shouldCreateAnomalyWithAllRequiredFields() {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setId(1L);
        anomaly.setUser(user);
        anomaly.setDam(dam);
        anomaly.setLatitude(-23.5505);
        anomaly.setLongitude(-46.6333);
        anomaly.setOrigin(AnomalyOriginEnum.WEB);
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);

        assertThat(anomaly).satisfies(a -> {
            assertThat(a.getId()).isEqualTo(1L);
            assertThat(a.getUser()).isEqualTo(user);
            assertThat(a.getDam()).isEqualTo(dam);
            assertThat(a.getLatitude()).isEqualTo(-23.5505);
            assertThat(a.getLongitude()).isEqualTo(-46.6333);
            assertThat(a.getOrigin()).isEqualTo(AnomalyOriginEnum.WEB);
            assertThat(a.getDangerLevel()).isEqualTo(dangerLevel);
            assertThat(a.getStatus()).isEqualTo(status);
            assertThat(a.getPhotos()).isNotNull().isEmpty();
        });
    }

    @Test
    @DisplayName("Should create anomaly with optional fields")
    void shouldCreateAnomalyWithOptionalFields() {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setObservation("Trinca detectada na estrutura");
        anomaly.setRecommendation("Realizar inspeção técnica detalhada");
        anomaly.setQuestionnaireId(100L);
        anomaly.setQuestionId(50L);

        assertThat(anomaly).satisfies(a -> {
            assertThat(a.getObservation()).isEqualTo("Trinca detectada na estrutura");
            assertThat(a.getRecommendation()).isEqualTo("Realizar inspeção técnica detalhada");
            assertThat(a.getQuestionnaireId()).isEqualTo(100L);
            assertThat(a.getQuestionId()).isEqualTo(50L);
        });
    }

    @Test
    @DisplayName("Should initialize photos as empty HashSet")
    void shouldInitializePhotosAsEmptyHashSet() {

        AnomalyEntity anomaly = new AnomalyEntity();

        assertThat(anomaly.getPhotos())
                .isNotNull()
                .isInstanceOf(HashSet.class)
                .isEmpty();
    }

    @Test
    @DisplayName("Should add photo to anomaly")
    void shouldAddPhotoToAnomaly() {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setId(1L);

        AnomalyPhotoEntity photo = new AnomalyPhotoEntity();
        photo.setId(1L);
        photo.setAnomaly(anomaly);
        photo.setImagePath("/uploads/anomalies/photo1.jpg");
        photo.setDamId(dam.getId());

        anomaly.getPhotos().add(photo);

        assertThat(anomaly.getPhotos())
                .hasSize(1)
                .contains(photo);
        assertThat(photo.getAnomaly()).isEqualTo(anomaly);
    }

    @Test
    @DisplayName("Should add multiple photos to anomaly")
    void shouldAddMultiplePhotosToAnomaly() {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setId(1L);

        AnomalyPhotoEntity photo1 = new AnomalyPhotoEntity();
        photo1.setId(1L);
        photo1.setAnomaly(anomaly);
        photo1.setImagePath("/uploads/anomalies/photo1.jpg");

        AnomalyPhotoEntity photo2 = new AnomalyPhotoEntity();
        photo2.setId(2L);
        photo2.setAnomaly(anomaly);
        photo2.setImagePath("/uploads/anomalies/photo2.jpg");

        anomaly.getPhotos().add(photo1);
        anomaly.getPhotos().add(photo2);

        assertThat(anomaly.getPhotos())
                .hasSize(2)
                .containsExactlyInAnyOrder(photo1, photo2);
    }

    @Test
    @DisplayName("Should handle all anomaly origin types")
    void shouldHandleAllAnomalyOriginTypes() {

        AnomalyEntity checklist = new AnomalyEntity();
        checklist.setOrigin(AnomalyOriginEnum.CHECKLIST);

        AnomalyEntity web = new AnomalyEntity();
        web.setOrigin(AnomalyOriginEnum.WEB);

        AnomalyEntity other = new AnomalyEntity();
        other.setOrigin(AnomalyOriginEnum.OTHER);

        assertThat(checklist.getOrigin()).isEqualTo(AnomalyOriginEnum.CHECKLIST);
        assertThat(web.getOrigin()).isEqualTo(AnomalyOriginEnum.WEB);
        assertThat(other.getOrigin()).isEqualTo(AnomalyOriginEnum.OTHER);
    }

    @Test
    @DisplayName("Should store questionnaire and question IDs for checklist origin")
    void shouldStoreQuestionnaireAndQuestionIdsForChecklistOrigin() {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setOrigin(AnomalyOriginEnum.CHECKLIST);
        anomaly.setQuestionnaireId(100L);
        anomaly.setQuestionId(50L);

        assertThat(anomaly.getOrigin()).isEqualTo(AnomalyOriginEnum.CHECKLIST);
        assertThat(anomaly.getQuestionnaireId()).isEqualTo(100L);
        assertThat(anomaly.getQuestionId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Should validate geographic coordinates")
    void shouldValidateGeographicCoordinates() {

        AnomalyEntity anomaly = new AnomalyEntity();

        anomaly.setLatitude(-23.5505);
        anomaly.setLongitude(-46.6333);

        assertThat(anomaly.getLatitude()).isBetween(-90.0, 90.0);
        assertThat(anomaly.getLongitude()).isBetween(-180.0, 180.0);
    }

    @Test
    @DisplayName("Should maintain relationship with user")
    void shouldMaintainRelationshipWithUser() {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setUser(user);

        assertThat(anomaly.getUser())
                .isNotNull()
                .isEqualTo(user);
        assertThat(anomaly.getUser().getName()).isEqualTo(user.getName());
    }

    @Test
    @DisplayName("Should maintain relationship with dam")
    void shouldMaintainRelationshipWithDam() {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setDam(dam);

        assertThat(anomaly.getDam())
                .isNotNull()
                .isEqualTo(dam);
        assertThat(anomaly.getDam().getName()).isEqualTo("Barragem Principal");
    }

    @Test
    @DisplayName("Should maintain relationship with danger level")
    void shouldMaintainRelationshipWithDangerLevel() {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setDangerLevel(dangerLevel);

        assertThat(anomaly.getDangerLevel())
                .isNotNull()
                .isEqualTo(dangerLevel);
        assertThat(anomaly.getDangerLevel().getName()).isEqualTo("Alto");
    }

    @Test
    @DisplayName("Should maintain relationship with status")
    void shouldMaintainRelationshipWithStatus() {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setStatus(status);

        assertThat(anomaly.getStatus())
                .isNotNull()
                .isEqualTo(status);
        assertThat(anomaly.getStatus().getName()).isEqualTo("Aberta");
    }

    @Test
    @DisplayName("Should handle createdAt timestamp")
    void shouldHandleCreatedAtTimestamp() {

        AnomalyEntity anomaly = new AnomalyEntity();
        LocalDateTime now = LocalDateTime.now();
        anomaly.setCreatedAt(now);

        assertThat(anomaly.getCreatedAt())
                .isNotNull()
                .isEqualTo(now);
    }

    @Test
    @DisplayName("Should create anomaly using all args constructor")
    void shouldCreateAnomalyUsingAllArgsConstructor() {

        Set<AnomalyPhotoEntity> photos = new HashSet<>();
        AnomalyEntity anomaly = new AnomalyEntity(
                1L,
                user,
                dam,
                LocalDateTime.now(),
                -23.5505,
                -46.6333,
                100L,
                50L,
                AnomalyOriginEnum.CHECKLIST,
                "Observação importante",
                "Recomendação crítica",
                dangerLevel,
                status,
                photos
        );

        assertThat(anomaly).satisfies(a -> {
            assertThat(a.getId()).isEqualTo(1L);
            assertThat(a.getUser()).isEqualTo(user);
            assertThat(a.getDam()).isEqualTo(dam);
            assertThat(a.getLatitude()).isEqualTo(-23.5505);
            assertThat(a.getLongitude()).isEqualTo(-46.6333);
            assertThat(a.getQuestionnaireId()).isEqualTo(100L);
            assertThat(a.getQuestionId()).isEqualTo(50L);
            assertThat(a.getOrigin()).isEqualTo(AnomalyOriginEnum.CHECKLIST);
            assertThat(a.getObservation()).isEqualTo("Observação importante");
            assertThat(a.getRecommendation()).isEqualTo("Recomendação crítica");
            assertThat(a.getDangerLevel()).isEqualTo(dangerLevel);
            assertThat(a.getStatus()).isEqualTo(status);
            assertThat(a.getPhotos()).isEqualTo(photos);
        });
    }

    @Test
    @DisplayName("Should handle observation and recommendation as text")
    void shouldHandleObservationAndRecommendationAsText() {

        String longObservation = "Observação muito longa ".repeat(50);
        String longRecommendation = "Recomendação muito longa ".repeat(50);

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setObservation(longObservation);
        anomaly.setRecommendation(longRecommendation);

        assertThat(anomaly.getObservation()).hasSize(longObservation.length());
        assertThat(anomaly.getRecommendation()).hasSize(longRecommendation.length());
    }

    @Test
    @DisplayName("Should allow null for optional fields")
    void shouldAllowNullForOptionalFields() {

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setObservation(null);
        anomaly.setRecommendation(null);
        anomaly.setQuestionnaireId(null);
        anomaly.setQuestionId(null);

        assertThat(anomaly.getObservation()).isNull();
        assertThat(anomaly.getRecommendation()).isNull();
        assertThat(anomaly.getQuestionnaireId()).isNull();
        assertThat(anomaly.getQuestionId()).isNull();
    }
}
