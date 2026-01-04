package com.geosegbar.unit.infra.anomaly.services;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.common.enums.AnomalyOriginEnum;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyPhotoEntity;
import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.anomaly.dtos.AnomalyDTO;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.anomaly.services.AnomalyService;
import com.geosegbar.infra.anomaly_photo.persistence.jpa.AnomalyPhotoRepository;
import com.geosegbar.infra.anomaly_status.persistence.jpa.AnomalyStatusRepository;
import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.danger_level.persistence.jpa.DangerLevelRepository;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AnomalyServiceTest {

    @Mock
    private AnomalyRepository anomalyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DamRepository damRepository;

    @Mock
    private DangerLevelRepository dangerLevelRepository;

    @Mock
    private AnomalyStatusRepository statusRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AnomalyPhotoRepository anomalyPhotoRepository;

    @InjectMocks
    private AnomalyService anomalyService;

    private UserEntity mockUser;
    private DamEntity mockDam;
    private DangerLevelEntity mockDangerLevel;
    private AnomalyStatusEntity mockStatus;
    private AnomalyEntity mockAnomaly;
    private AnomalyDTO mockAnomalyDTO;

    @BeforeEach
    void setUp() {
        mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setName("Test User");

        mockDam = new DamEntity();
        mockDam.setId(1L);
        mockDam.setName("Test Dam");

        mockDangerLevel = new DangerLevelEntity();
        mockDangerLevel.setId(1L);
        mockDangerLevel.setName("Normal");

        mockStatus = new AnomalyStatusEntity();
        mockStatus.setId(1L);
        mockStatus.setName("Pendente");

        mockAnomaly = new AnomalyEntity();
        mockAnomaly.setId(1L);
        mockAnomaly.setUser(mockUser);
        mockAnomaly.setDam(mockDam);
        mockAnomaly.setLatitude(-23.550520);
        mockAnomaly.setLongitude(-46.633308);
        mockAnomaly.setObservation("Test observation");
        mockAnomaly.setDangerLevel(mockDangerLevel);
        mockAnomaly.setStatus(mockStatus);
        mockAnomaly.setPhotos(new HashSet<>());

        mockAnomalyDTO = new AnomalyDTO();
        mockAnomalyDTO.setUserId(1L);
        mockAnomalyDTO.setDamId(1L);
        mockAnomalyDTO.setDangerLevelId(1L);
        mockAnomalyDTO.setStatusId(1L);
        mockAnomalyDTO.setLatitude(-23.550520);
        mockAnomalyDTO.setLongitude(-46.633308);
        mockAnomalyDTO.setObservation("Test observation");
    }

    @Test
    void shouldInitializeDangerLevelsWhenCountIsZero() {
        when(dangerLevelRepository.count()).thenReturn(0L);

        anomalyService.init();

        verify(dangerLevelRepository, times(4)).save(any(DangerLevelEntity.class));
    }

    @Test
    void shouldNotInitializeDangerLevelsWhenCountIsNotZero() {
        when(dangerLevelRepository.count()).thenReturn(4L);
        when(statusRepository.count()).thenReturn(4L);

        anomalyService.init();

        verify(dangerLevelRepository, times(0)).save(any(DangerLevelEntity.class));
    }

    @Test
    void shouldInitializeAnomalyStatusesWhenCountIsZero() {
        when(dangerLevelRepository.count()).thenReturn(4L);
        when(statusRepository.count()).thenReturn(0L);

        anomalyService.init();

        verify(statusRepository, times(4)).save(any(AnomalyStatusEntity.class));
    }

    @Test
    void shouldNotInitializeAnomalyStatusesWhenCountIsNotZero() {
        when(dangerLevelRepository.count()).thenReturn(4L);
        when(statusRepository.count()).thenReturn(4L);

        anomalyService.init();

        verify(statusRepository, times(0)).save(any(AnomalyStatusEntity.class));
    }

    @Test
    void shouldFindAllAnomalies() {
        List<AnomalyEntity> anomalies = List.of(mockAnomaly);
        when(anomalyRepository.findAll()).thenReturn(anomalies);

        List<AnomalyEntity> result = anomalyService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(anomalyRepository).findAll();
    }

    @Test
    void shouldFindAnomalyById() {
        when(anomalyRepository.findById(1L)).thenReturn(Optional.of(mockAnomaly));

        AnomalyEntity result = anomalyService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getObservation()).isEqualTo("Test observation");
        verify(anomalyRepository).findById(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAnomalyNotFoundById() {
        when(anomalyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyService.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Anomalia não encontrada!");
    }

    @Test
    void shouldFindAnomaliesByDamId() {
        List<AnomalyEntity> anomalies = List.of(mockAnomaly);
        when(anomalyRepository.findByDamId(1L)).thenReturn(anomalies);

        List<AnomalyEntity> result = anomalyService.findByDamId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDam().getId()).isEqualTo(1L);
        verify(anomalyRepository).findByDamId(1L);
    }

    @Test
    void shouldCreateAnomalyWithoutPhotos() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));
        when(anomalyRepository.save(any(AnomalyEntity.class))).thenReturn(mockAnomaly);

        AnomalyEntity result = anomalyService.create(mockAnomalyDTO);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getObservation()).isEqualTo("Test observation");
        verify(anomalyRepository).save(any(AnomalyEntity.class));
    }

    @Test
    void shouldCreateAnomalyWithPhotos() {
        String base64Image = Base64.getEncoder().encodeToString("test image data".getBytes());
        PhotoSubmissionDTO photoDTO = new PhotoSubmissionDTO();
        photoDTO.setBase64Image("data:image/jpeg;base64," + base64Image);
        photoDTO.setFileName("test-photo.jpg");
        photoDTO.setContentType("image/jpeg");

        mockAnomalyDTO.setPhotos(List.of(photoDTO));

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));
        when(anomalyRepository.save(any(AnomalyEntity.class))).thenReturn(mockAnomaly);
        when(fileStorageService.storeFileFromBytes(any(byte[].class), anyString(), anyString(), eq("anomalies")))
                .thenReturn("https://test.com/api/files/anomalies/test-photo.jpg");
        when(anomalyPhotoRepository.save(any(AnomalyPhotoEntity.class)))
                .thenReturn(new AnomalyPhotoEntity());

        AnomalyEntity result = anomalyService.create(mockAnomalyDTO);

        assertThat(result).isNotNull();
        verify(fileStorageService).storeFileFromBytes(any(byte[].class), eq("test-photo.jpg"),
                eq("image/jpeg"), eq("anomalies"));
        verify(anomalyPhotoRepository).save(any(AnomalyPhotoEntity.class));
    }

    @Test
    void shouldHandleBase64ImageWithoutDataPrefix() {
        String base64Image = Base64.getEncoder().encodeToString("test image data".getBytes());
        PhotoSubmissionDTO photoDTO = new PhotoSubmissionDTO();
        photoDTO.setBase64Image(base64Image);
        photoDTO.setFileName("test-photo.jpg");
        photoDTO.setContentType("image/jpeg");

        mockAnomalyDTO.setPhotos(List.of(photoDTO));

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));
        when(anomalyRepository.save(any(AnomalyEntity.class))).thenReturn(mockAnomaly);
        when(fileStorageService.storeFileFromBytes(any(byte[].class), anyString(), anyString(), eq("anomalies")))
                .thenReturn("https://test.com/api/files/anomalies/test-photo.jpg");
        when(anomalyPhotoRepository.save(any(AnomalyPhotoEntity.class)))
                .thenReturn(new AnomalyPhotoEntity());

        AnomalyEntity result = anomalyService.create(mockAnomalyDTO);

        assertThat(result).isNotNull();
        verify(fileStorageService).storeFileFromBytes(any(byte[].class), anyString(), anyString(), eq("anomalies"));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserNotFoundDuringCreate() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyService.create(mockAnomalyDTO))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Usuário não encontrado!");
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDamNotFoundDuringCreate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyService.create(mockAnomalyDTO))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Barragem não encontrada!");
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDangerLevelNotFoundDuringCreate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyService.create(mockAnomalyDTO))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Nível de perigo não encontrado!");
    }

    @Test
    void shouldThrowNotFoundExceptionWhenStatusNotFoundDuringCreate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyService.create(mockAnomalyDTO))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Status não encontrado!");
    }

    @Test
    void shouldUpdateAnomalyWithoutPhotos() {
        when(anomalyRepository.findById(1L)).thenReturn(Optional.of(mockAnomaly));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));
        when(anomalyRepository.save(any(AnomalyEntity.class))).thenReturn(mockAnomaly);

        mockAnomalyDTO.setObservation("Updated observation");

        AnomalyEntity result = anomalyService.update(1L, mockAnomalyDTO);

        assertThat(result).isNotNull();
        verify(anomalyRepository).save(any(AnomalyEntity.class));
    }

    @Test
    void shouldUpdateAnomalyWithPhotosAndDeleteOldPhotos() {
        AnomalyPhotoEntity oldPhoto = new AnomalyPhotoEntity();
        oldPhoto.setId(1L);
        oldPhoto.setImagePath("https://test.com/api/files/anomalies/old-photo.jpg");
        mockAnomaly.getPhotos().add(oldPhoto);

        String base64Image = Base64.getEncoder().encodeToString("new image data".getBytes());
        PhotoSubmissionDTO photoDTO = new PhotoSubmissionDTO();
        photoDTO.setBase64Image("data:image/jpeg;base64," + base64Image);
        photoDTO.setFileName("new-photo.jpg");
        photoDTO.setContentType("image/jpeg");

        mockAnomalyDTO.setPhotos(List.of(photoDTO));

        when(anomalyRepository.findById(1L)).thenReturn(Optional.of(mockAnomaly));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));
        when(anomalyRepository.save(any(AnomalyEntity.class))).thenReturn(mockAnomaly);
        when(fileStorageService.storeFileFromBytes(any(byte[].class), anyString(), anyString(), eq("anomalies")))
                .thenReturn("https://test.com/api/files/anomalies/new-photo.jpg");
        when(anomalyPhotoRepository.save(any(AnomalyPhotoEntity.class)))
                .thenReturn(new AnomalyPhotoEntity());

        AnomalyEntity result = anomalyService.update(1L, mockAnomalyDTO);

        assertThat(result).isNotNull();
        verify(fileStorageService).deleteFile("https://test.com/api/files/anomalies/old-photo.jpg");
        verify(anomalyPhotoRepository).delete(oldPhoto);
        verify(fileStorageService).storeFileFromBytes(any(byte[].class), eq("new-photo.jpg"),
                eq("image/jpeg"), eq("anomalies"));
    }

    @Test
    void shouldDeleteAnomalyAndPhotos() {
        AnomalyPhotoEntity photo1 = new AnomalyPhotoEntity();
        photo1.setId(1L);
        photo1.setImagePath("https://test.com/api/files/anomalies/photo1.jpg");

        AnomalyPhotoEntity photo2 = new AnomalyPhotoEntity();
        photo2.setId(2L);
        photo2.setImagePath("https://test.com/api/files/anomalies/photo2.jpg");

        mockAnomaly.getPhotos().add(photo1);
        mockAnomaly.getPhotos().add(photo2);

        when(anomalyRepository.findById(1L)).thenReturn(Optional.of(mockAnomaly));

        anomalyService.delete(1L);

        verify(fileStorageService).deleteFile("https://test.com/api/files/anomalies/photo1.jpg");
        verify(fileStorageService).deleteFile("https://test.com/api/files/anomalies/photo2.jpg");
        verify(anomalyRepository).delete(mockAnomaly);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAnomalyNotFoundDuringDelete() {
        when(anomalyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> anomalyService.delete(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Anomalia não encontrada!");
    }

    @Test
    void shouldCreateAnomalyWithQuestionnaireAndQuestionIds() {
        mockAnomalyDTO.setQuestionnaireId(10L);
        mockAnomalyDTO.setQuestionId(20L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));
        when(anomalyRepository.save(any(AnomalyEntity.class))).thenReturn(mockAnomaly);

        AnomalyEntity result = anomalyService.create(mockAnomalyDTO);

        assertThat(result).isNotNull();
        verify(anomalyRepository).save(any(AnomalyEntity.class));
    }

    @Test
    void shouldCreateAnomalyWithOriginField() {
        mockAnomalyDTO.setOrigin(AnomalyOriginEnum.CHECKLIST);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));
        when(anomalyRepository.save(any(AnomalyEntity.class))).thenReturn(mockAnomaly);

        AnomalyEntity result = anomalyService.create(mockAnomalyDTO);

        assertThat(result).isNotNull();
        verify(anomalyRepository).save(any(AnomalyEntity.class));
    }

    @Test
    void shouldCreateAnomalyWithRecommendationField() {
        mockAnomalyDTO.setRecommendation("Realizar vistoria detalhada na área afetada");

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));
        when(anomalyRepository.save(any(AnomalyEntity.class))).thenReturn(mockAnomaly);

        AnomalyEntity result = anomalyService.create(mockAnomalyDTO);

        assertThat(result).isNotNull();
        verify(anomalyRepository).save(any(AnomalyEntity.class));
    }

    @Test
    void shouldCreateAnomalyWithNegativeCoordinates() {
        mockAnomalyDTO.setLatitude(-34.603722);
        mockAnomalyDTO.setLongitude(-58.381592);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));
        when(anomalyRepository.save(any(AnomalyEntity.class))).thenReturn(mockAnomaly);

        AnomalyEntity result = anomalyService.create(mockAnomalyDTO);

        assertThat(result).isNotNull();
        verify(anomalyRepository).save(any(AnomalyEntity.class));
    }

    @Test
    void shouldThrowFileStorageExceptionWhenPhotoProcessingFails() {
        PhotoSubmissionDTO photoDTO = new PhotoSubmissionDTO();
        photoDTO.setBase64Image("invalid-base64-data");
        photoDTO.setFileName("test-photo.jpg");
        photoDTO.setContentType("image/jpeg");

        mockAnomalyDTO.setPhotos(List.of(photoDTO));

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(damRepository.findById(1L)).thenReturn(Optional.of(mockDam));
        when(dangerLevelRepository.findById(1L)).thenReturn(Optional.of(mockDangerLevel));
        when(statusRepository.findById(1L)).thenReturn(Optional.of(mockStatus));
        when(anomalyRepository.save(any(AnomalyEntity.class))).thenReturn(mockAnomaly);

        assertThatThrownBy(() -> anomalyService.create(mockAnomalyDTO))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Erro ao processar imagem da anomalia");
    }
}
