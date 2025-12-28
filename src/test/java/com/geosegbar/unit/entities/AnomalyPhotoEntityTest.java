package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyPhotoEntity;

@DisplayName("Unit Tests - AnomalyPhotoEntity")
class AnomalyPhotoEntityTest extends BaseUnitTest {

    private AnomalyEntity anomaly;

    @BeforeEach
    void setUp() {
        anomaly = new AnomalyEntity();
        anomaly.setId(1L);
    }

    @Test
    @DisplayName("Should create anomaly photo with all required fields")
    void shouldCreateAnomalyPhotoWithAllRequiredFields() {
        // Given
        AnomalyPhotoEntity photo = new AnomalyPhotoEntity();
        photo.setId(1L);
        photo.setAnomaly(anomaly);
        photo.setImagePath("/uploads/anomalies/photo1.jpg");
        photo.setDamId(100L);

        // Then
        assertThat(photo).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getAnomaly()).isEqualTo(anomaly);
            assertThat(p.getImagePath()).isEqualTo("/uploads/anomalies/photo1.jpg");
            assertThat(p.getDamId()).isEqualTo(100L);
        });
    }

    @Test
    @DisplayName("Should create anomaly photo using all args constructor")
    void shouldCreateAnomalyPhotoUsingAllArgsConstructor() {
        // Given & When
        AnomalyPhotoEntity photo = new AnomalyPhotoEntity(
                1L,
                anomaly,
                "/uploads/anomalies/photo1.jpg",
                100L
        );

        // Then
        assertThat(photo).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getAnomaly()).isEqualTo(anomaly);
            assertThat(p.getImagePath()).isEqualTo("/uploads/anomalies/photo1.jpg");
            assertThat(p.getDamId()).isEqualTo(100L);
        });
    }

    @Test
    @DisplayName("Should maintain bidirectional relationship with anomaly")
    void shouldMaintainBidirectionalRelationshipWithAnomaly() {
        // Given
        AnomalyPhotoEntity photo = new AnomalyPhotoEntity();
        photo.setId(1L);
        photo.setAnomaly(anomaly);
        photo.setImagePath("/uploads/anomalies/photo1.jpg");

        // When
        anomaly.getPhotos().add(photo);

        // Then
        assertThat(photo.getAnomaly()).isEqualTo(anomaly);
        assertThat(anomaly.getPhotos()).contains(photo);
    }

    @Test
    @DisplayName("Should handle different image path formats")
    void shouldHandleDifferentImagePathFormats() {
        // Given
        AnomalyPhotoEntity photo1 = new AnomalyPhotoEntity();
        photo1.setImagePath("/uploads/anomalies/photo1.jpg");

        AnomalyPhotoEntity photo2 = new AnomalyPhotoEntity();
        photo2.setImagePath("/uploads/anomalies/2024/12/photo2.png");

        AnomalyPhotoEntity photo3 = new AnomalyPhotoEntity();
        photo3.setImagePath("https://cdn.example.com/anomalies/photo3.jpg");

        // Then
        assertThat(photo1.getImagePath()).isEqualTo("/uploads/anomalies/photo1.jpg");
        assertThat(photo2.getImagePath()).isEqualTo("/uploads/anomalies/2024/12/photo2.png");
        assertThat(photo3.getImagePath()).isEqualTo("https://cdn.example.com/anomalies/photo3.jpg");
    }

    @Test
    @DisplayName("Should store damId for easier querying")
    void shouldStoreDamIdForEasierQuerying() {
        // Given
        AnomalyPhotoEntity photo = new AnomalyPhotoEntity();
        photo.setDamId(100L);

        // Then
        assertThat(photo.getDamId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should allow null damId")
    void shouldAllowNullDamId() {
        // Given
        AnomalyPhotoEntity photo = new AnomalyPhotoEntity();
        photo.setDamId(null);

        // Then
        assertThat(photo.getDamId()).isNull();
    }

    @Test
    @DisplayName("Should handle multiple photos for same anomaly")
    void shouldHandleMultiplePhotosForSameAnomaly() {
        // Given
        AnomalyPhotoEntity photo1 = new AnomalyPhotoEntity();
        photo1.setId(1L);
        photo1.setAnomaly(anomaly);
        photo1.setImagePath("/uploads/anomalies/photo1.jpg");

        AnomalyPhotoEntity photo2 = new AnomalyPhotoEntity();
        photo2.setId(2L);
        photo2.setAnomaly(anomaly);
        photo2.setImagePath("/uploads/anomalies/photo2.jpg");

        AnomalyPhotoEntity photo3 = new AnomalyPhotoEntity();
        photo3.setId(3L);
        photo3.setAnomaly(anomaly);
        photo3.setImagePath("/uploads/anomalies/photo3.jpg");

        // When
        anomaly.getPhotos().add(photo1);
        anomaly.getPhotos().add(photo2);
        anomaly.getPhotos().add(photo3);

        // Then
        assertThat(anomaly.getPhotos())
                .hasSize(3)
                .containsExactlyInAnyOrder(photo1, photo2, photo3);

        assertThat(photo1.getAnomaly()).isEqualTo(anomaly);
        assertThat(photo2.getAnomaly()).isEqualTo(anomaly);
        assertThat(photo3.getAnomaly()).isEqualTo(anomaly);
    }

    @Test
    @DisplayName("Should support different image extensions")
    void shouldSupportDifferentImageExtensions() {
        // Given
        AnomalyPhotoEntity jpg = new AnomalyPhotoEntity();
        jpg.setImagePath("/uploads/anomalies/photo.jpg");

        AnomalyPhotoEntity png = new AnomalyPhotoEntity();
        png.setImagePath("/uploads/anomalies/photo.png");

        AnomalyPhotoEntity jpeg = new AnomalyPhotoEntity();
        jpeg.setImagePath("/uploads/anomalies/photo.jpeg");

        AnomalyPhotoEntity webp = new AnomalyPhotoEntity();
        webp.setImagePath("/uploads/anomalies/photo.webp");

        // Then
        assertThat(jpg.getImagePath()).endsWith(".jpg");
        assertThat(png.getImagePath()).endsWith(".png");
        assertThat(jpeg.getImagePath()).endsWith(".jpeg");
        assertThat(webp.getImagePath()).endsWith(".webp");
    }

    @Test
    @DisplayName("Should maintain photo identity across changes")
    void shouldMaintainPhotoIdentityAcrossChanges() {
        // Given
        AnomalyPhotoEntity photo = new AnomalyPhotoEntity();
        photo.setId(1L);
        photo.setImagePath("/uploads/anomalies/old-path.jpg");
        photo.setDamId(100L);

        Long originalId = photo.getId();

        // When
        photo.setImagePath("/uploads/anomalies/new-path.jpg");
        photo.setDamId(200L);

        // Then
        assertThat(photo.getId()).isEqualTo(originalId);
        assertThat(photo.getImagePath()).isEqualTo("/uploads/anomalies/new-path.jpg");
        assertThat(photo.getDamId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("Should create photo with no args constructor")
    void shouldCreatePhotoWithNoArgsConstructor() {
        // Given & When
        AnomalyPhotoEntity photo = new AnomalyPhotoEntity();

        // Then
        assertThat(photo).isNotNull();
        assertThat(photo.getId()).isNull();
        assertThat(photo.getAnomaly()).isNull();
        assertThat(photo.getImagePath()).isNull();
        assertThat(photo.getDamId()).isNull();
    }
}
