package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.AnswerPhotoEntity;

@DisplayName("Unit Tests - AnswerPhotoEntity")
class AnswerPhotoEntityTest extends BaseUnitTest {

    private AnswerEntity answer;

    @BeforeEach
    void setUp() {
        answer = new AnswerEntity();
        answer.setId(1L);
    }

    @Test
    @DisplayName("Should create answer photo with all fields")
    void shouldCreateAnswerPhotoWithAllFields() {
        // Given
        AnswerPhotoEntity photo = new AnswerPhotoEntity();
        photo.setId(1L);
        photo.setAnswer(answer);
        photo.setImagePath("/uploads/answers/photo1.jpg");

        // Then
        assertThat(photo).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getAnswer()).isEqualTo(answer);
            assertThat(p.getImagePath()).isEqualTo("/uploads/answers/photo1.jpg");
        });
    }

    @Test
    @DisplayName("Should create answer photo using all args constructor")
    void shouldCreateAnswerPhotoUsingAllArgsConstructor() {
        // Given & When
        AnswerPhotoEntity photo = new AnswerPhotoEntity(
                1L,
                answer,
                "/uploads/answers/photo1.jpg"
        );

        // Then
        assertThat(photo).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getAnswer()).isEqualTo(answer);
            assertThat(p.getImagePath()).isEqualTo("/uploads/answers/photo1.jpg");
        });
    }

    @Test
    @DisplayName("Should create answer photo with no args constructor")
    void shouldCreateAnswerPhotoWithNoArgsConstructor() {
        // Given & When
        AnswerPhotoEntity photo = new AnswerPhotoEntity();

        // Then
        assertThat(photo).isNotNull();
        assertThat(photo.getId()).isNull();
        assertThat(photo.getAnswer()).isNull();
        assertThat(photo.getImagePath()).isNull();
    }

    @Test
    @DisplayName("Should maintain bidirectional relationship with answer")
    void shouldMaintainBidirectionalRelationshipWithAnswer() {
        // Given
        AnswerPhotoEntity photo = new AnswerPhotoEntity();
        photo.setId(1L);
        photo.setAnswer(answer);
        photo.setImagePath("/uploads/answers/photo1.jpg");

        // When
        answer.getPhotos().add(photo);

        // Then
        assertThat(photo.getAnswer()).isEqualTo(answer);
        assertThat(answer.getPhotos()).contains(photo);
    }

    @Test
    @DisplayName("Should handle different image path formats")
    void shouldHandleDifferentImagePathFormats() {
        // Given
        AnswerPhotoEntity photo1 = new AnswerPhotoEntity();
        photo1.setImagePath("/uploads/answers/photo1.jpg");

        AnswerPhotoEntity photo2 = new AnswerPhotoEntity();
        photo2.setImagePath("/uploads/answers/2024/12/photo2.png");

        AnswerPhotoEntity photo3 = new AnswerPhotoEntity();
        photo3.setImagePath("https://cdn.example.com/answers/photo3.jpg");

        AnswerPhotoEntity photo4 = new AnswerPhotoEntity();
        photo4.setImagePath("/storage/answers/questionnaire-100/answer-50/image.jpg");

        // Then
        assertThat(photo1.getImagePath()).isEqualTo("/uploads/answers/photo1.jpg");
        assertThat(photo2.getImagePath()).isEqualTo("/uploads/answers/2024/12/photo2.png");
        assertThat(photo3.getImagePath()).isEqualTo("https://cdn.example.com/answers/photo3.jpg");
        assertThat(photo4.getImagePath()).contains("questionnaire-100").contains("answer-50");
    }

    @Test
    @DisplayName("Should support different image extensions")
    void shouldSupportDifferentImageExtensions() {
        // Given
        AnswerPhotoEntity jpg = new AnswerPhotoEntity();
        jpg.setImagePath("/uploads/answers/photo.jpg");

        AnswerPhotoEntity png = new AnswerPhotoEntity();
        png.setImagePath("/uploads/answers/photo.png");

        AnswerPhotoEntity jpeg = new AnswerPhotoEntity();
        jpeg.setImagePath("/uploads/answers/photo.jpeg");

        AnswerPhotoEntity webp = new AnswerPhotoEntity();
        webp.setImagePath("/uploads/answers/photo.webp");

        AnswerPhotoEntity heic = new AnswerPhotoEntity();
        heic.setImagePath("/uploads/answers/photo.heic");

        // Then
        assertThat(jpg.getImagePath()).endsWith(".jpg");
        assertThat(png.getImagePath()).endsWith(".png");
        assertThat(jpeg.getImagePath()).endsWith(".jpeg");
        assertThat(webp.getImagePath()).endsWith(".webp");
        assertThat(heic.getImagePath()).endsWith(".heic");
    }

    @Test
    @DisplayName("Should handle multiple photos for same answer")
    void shouldHandleMultiplePhotosForSameAnswer() {
        // Given
        AnswerPhotoEntity photo1 = new AnswerPhotoEntity();
        photo1.setId(1L);
        photo1.setAnswer(answer);
        photo1.setImagePath("/uploads/answers/photo1.jpg");

        AnswerPhotoEntity photo2 = new AnswerPhotoEntity();
        photo2.setId(2L);
        photo2.setAnswer(answer);
        photo2.setImagePath("/uploads/answers/photo2.jpg");

        AnswerPhotoEntity photo3 = new AnswerPhotoEntity();
        photo3.setId(3L);
        photo3.setAnswer(answer);
        photo3.setImagePath("/uploads/answers/photo3.jpg");

        // When
        answer.getPhotos().add(photo1);
        answer.getPhotos().add(photo2);
        answer.getPhotos().add(photo3);

        // Then
        assertThat(answer.getPhotos())
                .hasSize(3)
                .containsExactlyInAnyOrder(photo1, photo2, photo3);

        assertThat(photo1.getAnswer()).isEqualTo(answer);
        assertThat(photo2.getAnswer()).isEqualTo(answer);
        assertThat(photo3.getAnswer()).isEqualTo(answer);
    }

    @Test
    @DisplayName("Should maintain photo identity across changes")
    void shouldMaintainPhotoIdentityAcrossChanges() {
        // Given
        AnswerPhotoEntity photo = new AnswerPhotoEntity();
        photo.setId(1L);
        photo.setImagePath("/uploads/answers/old-path.jpg");

        Long originalId = photo.getId();

        // When
        photo.setImagePath("/uploads/answers/new-path.jpg");

        // Then
        assertThat(photo.getId()).isEqualTo(originalId);
        assertThat(photo.getImagePath()).isEqualTo("/uploads/answers/new-path.jpg");
    }

    @Test
    @DisplayName("Should support photo path with special characters")
    void shouldSupportPhotoPathWithSpecialCharacters() {
        // Given
        AnswerPhotoEntity photo1 = new AnswerPhotoEntity();
        photo1.setImagePath("/uploads/answers/photo-estrutura-barragem_2024.jpg");

        AnswerPhotoEntity photo2 = new AnswerPhotoEntity();
        photo2.setImagePath("/uploads/answers/foto (1).jpg");

        AnswerPhotoEntity photo3 = new AnswerPhotoEntity();
        photo3.setImagePath("/uploads/answers/imagem-20241227-155030.jpg");

        // Then
        assertThat(photo1.getImagePath()).contains("-", "_");
        assertThat(photo2.getImagePath()).contains("(", ")");
        assertThat(photo3.getImagePath()).contains("-");
    }

    @Test
    @DisplayName("Should support photo with timestamp in filename")
    void shouldSupportPhotoWithTimestampInFilename() {
        // Given
        AnswerPhotoEntity photo = new AnswerPhotoEntity();
        photo.setImagePath("/uploads/answers/photo-20241227-155030-123456.jpg");

        // Then
        assertThat(photo.getImagePath())
                .contains("20241227")
                .contains("155030")
                .contains("123456");
    }

    @Test
    @DisplayName("Should support photo organized by questionnaire and answer")
    void shouldSupportPhotoOrganizedByQuestionnaireAndAnswer() {
        // Given
        AnswerPhotoEntity photo = new AnswerPhotoEntity();
        photo.setImagePath("/uploads/questionnaire-100/answer-50/photo-001.jpg");

        // Then
        assertThat(photo.getImagePath())
                .contains("questionnaire-100")
                .contains("answer-50")
                .contains("photo-001");
    }

    @Test
    @DisplayName("Should handle answer change")
    void shouldHandleAnswerChange() {
        // Given
        AnswerEntity answer1 = new AnswerEntity();
        answer1.setId(1L);

        AnswerEntity answer2 = new AnswerEntity();
        answer2.setId(2L);

        AnswerPhotoEntity photo = new AnswerPhotoEntity();
        photo.setAnswer(answer1);

        // When
        answer1.getPhotos().add(photo);
        answer1.getPhotos().remove(photo);
        photo.setAnswer(answer2);
        answer2.getPhotos().add(photo);

        // Then
        assertThat(photo.getAnswer()).isEqualTo(answer2);
        assertThat(answer1.getPhotos()).doesNotContain(photo);
        assertThat(answer2.getPhotos()).contains(photo);
    }

    @Test
    @DisplayName("Should support photo with long path")
    void shouldSupportPhotoWithLongPath() {
        // Given
        String longPath = "/uploads/answers/very/deep/nested/directory/structure/for/organizing/photos/by/date/and/questionnaire/and/answer/photo-123.jpg";

        AnswerPhotoEntity photo = new AnswerPhotoEntity();
        photo.setImagePath(longPath);

        // Then
        assertThat(photo.getImagePath()).hasSize(longPath.length());
        assertThat(photo.getImagePath()).endsWith("photo-123.jpg");
    }

    @Test
    @DisplayName("Should support cloud storage URLs")
    void shouldSupportCloudStorageUrls() {
        // Given
        AnswerPhotoEntity s3Photo = new AnswerPhotoEntity();
        s3Photo.setImagePath("https://s3.amazonaws.com/geosegbar-bucket/answers/photo1.jpg");

        AnswerPhotoEntity azurePhoto = new AnswerPhotoEntity();
        azurePhoto.setImagePath("https://geosegbar.blob.core.windows.net/answers/photo2.jpg");

        AnswerPhotoEntity gcsPhoto = new AnswerPhotoEntity();
        gcsPhoto.setImagePath("https://storage.googleapis.com/geosegbar-bucket/answers/photo3.jpg");

        // Then
        assertThat(s3Photo.getImagePath()).startsWith("https://s3.amazonaws.com");
        assertThat(azurePhoto.getImagePath()).startsWith("https://geosegbar.blob.core.windows.net");
        assertThat(gcsPhoto.getImagePath()).startsWith("https://storage.googleapis.com");
    }
}
