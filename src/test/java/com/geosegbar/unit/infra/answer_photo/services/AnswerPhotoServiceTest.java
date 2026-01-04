package com.geosegbar.unit.infra.answer_photo.services;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.AnswerPhotoEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.answer_photo.persistence.jpa.AnswerPhotoRepository;
import com.geosegbar.infra.answer_photo.services.AnswerPhotoService;
import com.geosegbar.infra.file_storage.FileStorageService;

/**
 * Testes unitários para AnswerPhotoService
 *
 * Cobertura: - CRUD operations (save/update/delete) - Photo management
 * (upload/replace) - Find operations (findById/findAll/findByAnswerId) -
 * FileStorageService integration - NotFoundException scenarios - MultipartFile
 * handling
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("AnswerPhotoService Unit Tests")
class AnswerPhotoServiceTest extends BaseUnitTest {

    @Mock
    private AnswerPhotoRepository answerPhotoRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private AnswerPhotoService answerPhotoService;

    private AnswerEntity mockAnswer;
    private AnswerPhotoEntity mockPhoto;

    @BeforeEach
    void setUp() {
        // Mock answer
        mockAnswer = new AnswerEntity();
        mockAnswer.setId(1L);

        // Mock photo
        mockPhoto = new AnswerPhotoEntity();
        mockPhoto.setId(1L);
        mockPhoto.setAnswer(mockAnswer);
        mockPhoto.setImagePath("https://test.com/api/files/answer-photos/123_photo.jpg");
    }

    // ==================== Save Photo Tests ====================
    @Test
    @DisplayName("Should save photo successfully")
    void shouldSavePhotoSuccessfully() {
        // Given
        Long answerId = 1L;
        String expectedUrl = "https://test.com/api/files/answer-photos/photo1.jpg";

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(mockAnswer));
        when(fileStorageService.storeFile(mockFile, "answer-photos")).thenReturn(expectedUrl);
        when(answerPhotoRepository.save(any(AnswerPhotoEntity.class))).thenAnswer(invocation -> {
            AnswerPhotoEntity photo = invocation.getArgument(0);
            photo.setId(1L);
            return photo;
        });

        // When
        AnswerPhotoEntity result = answerPhotoService.savePhoto(answerId, mockFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAnswer()).isEqualTo(mockAnswer);
        assertThat(result.getImagePath()).isEqualTo(expectedUrl);

        verify(answerRepository).findById(answerId);
        verify(fileStorageService).storeFile(mockFile, "answer-photos");
        verify(answerPhotoRepository).save(any(AnswerPhotoEntity.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when answer not found on save")
    void shouldThrowNotFoundExceptionWhenAnswerNotFoundOnSave() {
        // Given
        Long answerId = 999L;
        when(answerRepository.findById(answerId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> answerPhotoService.savePhoto(answerId, mockFile))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Resposta não encontrada!");
    }

    @Test
    @DisplayName("Should save multiple photos for same answer")
    void shouldSaveMultiplePhotosForSameAnswer() {
        // Given
        Long answerId = 1L;
        String url1 = "https://test.com/api/files/answer-photos/photo1.jpg";
        String url2 = "https://test.com/api/files/answer-photos/photo2.jpg";

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(mockAnswer));
        when(fileStorageService.storeFile(mockFile, "answer-photos"))
                .thenReturn(url1)
                .thenReturn(url2);
        when(answerPhotoRepository.save(any(AnswerPhotoEntity.class))).thenAnswer(invocation -> {
            AnswerPhotoEntity photo = invocation.getArgument(0);
            photo.setId(photo.getId() == null ? 1L : 2L);
            return photo;
        });

        // When
        AnswerPhotoEntity photo1 = answerPhotoService.savePhoto(answerId, mockFile);
        AnswerPhotoEntity photo2 = answerPhotoService.savePhoto(answerId, mockFile);

        // Then
        assertThat(photo1.getImagePath()).isEqualTo(url1);
        assertThat(photo2.getImagePath()).isEqualTo(url2);
        verify(answerRepository, times(2)).findById(answerId);
        verify(fileStorageService, times(2)).storeFile(mockFile, "answer-photos");
    }

    // ==================== Update Photo Tests ====================
    @Test
    @DisplayName("Should update photo entity successfully")
    void shouldUpdatePhotoEntitySuccessfully() {
        // Given
        AnswerPhotoEntity updatedPhoto = new AnswerPhotoEntity();
        updatedPhoto.setId(1L);
        updatedPhoto.setImagePath("https://test.com/api/files/answer-photos/updated.jpg");

        when(answerPhotoRepository.findById(1L)).thenReturn(Optional.of(mockPhoto));
        when(answerPhotoRepository.save(updatedPhoto)).thenReturn(updatedPhoto);

        // When
        AnswerPhotoEntity result = answerPhotoService.update(updatedPhoto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(answerPhotoRepository).findById(1L);
        verify(answerPhotoRepository).save(updatedPhoto);
    }

    @Test
    @DisplayName("Should throw NotFoundException when photo not found on update")
    void shouldThrowNotFoundExceptionWhenPhotoNotFoundOnUpdate() {
        // Given
        AnswerPhotoEntity updatedPhoto = new AnswerPhotoEntity();
        updatedPhoto.setId(999L);

        when(answerPhotoRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> answerPhotoService.update(updatedPhoto))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Foto da resposta não encontrada para atualização!");
    }

    @Test
    @DisplayName("Should update photo file and delete old file")
    void shouldUpdatePhotoFileAndDeleteOldFile() {
        // Given
        Long photoId = 1L;
        String oldImagePath = "https://test.com/api/files/answer-photos/old.jpg";
        String newImagePath = "https://test.com/api/files/answer-photos/new.jpg";

        mockPhoto.setImagePath(oldImagePath);

        when(answerPhotoRepository.findById(photoId)).thenReturn(Optional.of(mockPhoto));
        when(fileStorageService.storeFile(mockFile, "answer-photos")).thenReturn(newImagePath);
        when(answerPhotoRepository.save(any(AnswerPhotoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AnswerPhotoEntity result = answerPhotoService.updatePhoto(photoId, mockFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getImagePath()).isEqualTo(newImagePath);

        verify(fileStorageService).deleteFile(oldImagePath);
        verify(fileStorageService).storeFile(mockFile, "answer-photos");
        verify(answerPhotoRepository).save(mockPhoto);
    }

    @Test
    @DisplayName("Should throw NotFoundException when photo not found on updatePhoto")
    void shouldThrowNotFoundExceptionWhenPhotoNotFoundOnUpdatePhoto() {
        // Given
        Long photoId = 999L;
        when(answerPhotoRepository.findById(photoId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> answerPhotoService.updatePhoto(photoId, mockFile))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Foto da resposta não encontrada para atualização!");
    }

    @Test
    @DisplayName("Should handle null image path on update")
    void shouldHandleNullImagePathOnUpdate() {
        // Given
        Long photoId = 1L;
        String newImagePath = "https://test.com/api/files/answer-photos/new.jpg";

        mockPhoto.setImagePath(null);

        when(answerPhotoRepository.findById(photoId)).thenReturn(Optional.of(mockPhoto));
        when(fileStorageService.storeFile(mockFile, "answer-photos")).thenReturn(newImagePath);
        when(answerPhotoRepository.save(any(AnswerPhotoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AnswerPhotoEntity result = answerPhotoService.updatePhoto(photoId, mockFile);

        // Then
        assertThat(result.getImagePath()).isEqualTo(newImagePath);
        verify(fileStorageService, never()).deleteFile(anyString());
        verify(fileStorageService).storeFile(mockFile, "answer-photos");
    }

    // ==================== Delete Photo Tests ====================
    @Test
    @DisplayName("Should delete photo and file successfully")
    void shouldDeletePhotoAndFileSuccessfully() {
        // Given
        Long photoId = 1L;
        String imagePath = "https://test.com/api/files/answer-photos/photo.jpg";
        mockPhoto.setImagePath(imagePath);

        when(answerPhotoRepository.findById(photoId)).thenReturn(Optional.of(mockPhoto));
        doNothing().when(fileStorageService).deleteFile(imagePath);
        doNothing().when(answerPhotoRepository).deleteById(photoId);

        // When
        answerPhotoService.deleteById(photoId);

        // Then
        verify(answerPhotoRepository).findById(photoId);
        verify(fileStorageService).deleteFile(imagePath);
        verify(answerPhotoRepository).deleteById(photoId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when photo not found on delete")
    void shouldThrowNotFoundExceptionWhenPhotoNotFoundOnDelete() {
        // Given
        Long photoId = 999L;
        when(answerPhotoRepository.findById(photoId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> answerPhotoService.deleteById(photoId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Foto da resposta não encontrada para exclusão!");
    }

    @Test
    @DisplayName("Should handle null image path on delete")
    void shouldHandleNullImagePathOnDelete() {
        // Given
        Long photoId = 1L;
        mockPhoto.setImagePath(null);

        when(answerPhotoRepository.findById(photoId)).thenReturn(Optional.of(mockPhoto));
        doNothing().when(answerPhotoRepository).deleteById(photoId);

        // When
        answerPhotoService.deleteById(photoId);

        // Then
        verify(fileStorageService, never()).deleteFile(anyString());
        verify(answerPhotoRepository).deleteById(photoId);
    }

    // ==================== Find Tests ====================
    @Test
    @DisplayName("Should find photo by id successfully")
    void shouldFindPhotoByIdSuccessfully() {
        // Given
        Long photoId = 1L;
        when(answerPhotoRepository.findById(photoId)).thenReturn(Optional.of(mockPhoto));

        // When
        AnswerPhotoEntity result = answerPhotoService.findById(photoId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(photoId);
        assertThat(result.getImagePath()).isEqualTo(mockPhoto.getImagePath());
        verify(answerPhotoRepository).findById(photoId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when photo not found by id")
    void shouldThrowNotFoundExceptionWhenPhotoNotFoundById() {
        // Given
        Long photoId = 999L;
        when(answerPhotoRepository.findById(photoId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> answerPhotoService.findById(photoId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Foto da resposta não encontrada!");
    }

    @Test
    @DisplayName("Should find all photos successfully")
    void shouldFindAllPhotosSuccessfully() {
        // Given
        AnswerPhotoEntity photo1 = new AnswerPhotoEntity();
        photo1.setId(1L);
        photo1.setImagePath("https://test.com/api/files/answer-photos/photo1.jpg");

        AnswerPhotoEntity photo2 = new AnswerPhotoEntity();
        photo2.setId(2L);
        photo2.setImagePath("https://test.com/api/files/answer-photos/photo2.jpg");

        List<AnswerPhotoEntity> photos = Arrays.asList(photo1, photo2);

        when(answerPhotoRepository.findAll()).thenReturn(photos);

        // When
        List<AnswerPhotoEntity> result = answerPhotoService.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(photo1, photo2);
        verify(answerPhotoRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no photos exist")
    void shouldReturnEmptyListWhenNoPhotosExist() {
        // Given
        when(answerPhotoRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<AnswerPhotoEntity> result = answerPhotoService.findAll();

        // Then
        assertThat(result).isEmpty();
        verify(answerPhotoRepository).findAll();
    }

    @Test
    @DisplayName("Should find photos by answer id successfully")
    void shouldFindPhotosByAnswerIdSuccessfully() {
        // Given
        Long answerId = 1L;

        AnswerPhotoEntity photo1 = new AnswerPhotoEntity();
        photo1.setId(1L);
        photo1.setAnswer(mockAnswer);
        photo1.setImagePath("https://test.com/api/files/answer-photos/photo1.jpg");

        AnswerPhotoEntity photo2 = new AnswerPhotoEntity();
        photo2.setId(2L);
        photo2.setAnswer(mockAnswer);
        photo2.setImagePath("https://test.com/api/files/answer-photos/photo2.jpg");

        List<AnswerPhotoEntity> photos = Arrays.asList(photo1, photo2);

        when(answerPhotoRepository.findByAnswerId(answerId)).thenReturn(photos);

        // When
        List<AnswerPhotoEntity> result = answerPhotoService.findByAnswerId(answerId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(photo1, photo2);
        assertThat(result).allMatch(photo -> photo.getAnswer().getId().equals(answerId));
        verify(answerPhotoRepository).findByAnswerId(answerId);
    }

    @Test
    @DisplayName("Should return empty list when no photos for answer")
    void shouldReturnEmptyListWhenNoPhotosForAnswer() {
        // Given
        Long answerId = 1L;
        when(answerPhotoRepository.findByAnswerId(answerId)).thenReturn(Arrays.asList());

        // When
        List<AnswerPhotoEntity> result = answerPhotoService.findByAnswerId(answerId);

        // Then
        assertThat(result).isEmpty();
        verify(answerPhotoRepository).findByAnswerId(answerId);
    }

    @Test
    @DisplayName("Should handle Portuguese characters in image path")
    void shouldHandlePortugueseCharactersInImagePath() {
        // Given
        Long answerId = 1L;
        String imagePathWithPortuguese = "https://test.com/api/files/answer-photos/foto_resposta_situação.jpg";

        when(answerRepository.findById(answerId)).thenReturn(Optional.of(mockAnswer));
        when(fileStorageService.storeFile(mockFile, "answer-photos")).thenReturn(imagePathWithPortuguese);
        when(answerPhotoRepository.save(any(AnswerPhotoEntity.class))).thenAnswer(invocation -> {
            AnswerPhotoEntity photo = invocation.getArgument(0);
            photo.setId(1L);
            return photo;
        });

        // When
        AnswerPhotoEntity result = answerPhotoService.savePhoto(answerId, mockFile);

        // Then
        assertThat(result.getImagePath()).contains("situação");
    }
}
