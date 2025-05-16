package com.geosegbar.infra.anomaly.services;

import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.anomaly.dtos.AnomalyDTO;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.anomaly_status.persistence.jpa.AnomalyStatusRepository;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.danger_level.persistence.jpa.DangerLevelRepository;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnomalyService {

    private final AnomalyRepository anomalyRepository;
    private final UserRepository userRepository;
    private final DamRepository damRepository;
    private final DangerLevelRepository dangerLevelRepository;
    private final AnomalyStatusRepository statusRepository;
    private final FileStorageService fileStorageService;

    @PostConstruct
    public void init() {
        initDangerLevels();
        initAnomalyStatuses();
    }

    private void initDangerLevels() {
        if (dangerLevelRepository.count() == 0) {
            dangerLevelRepository.save(new DangerLevelEntity(null, "Normal", "Normal danger level"));
            dangerLevelRepository.save(new DangerLevelEntity(null, "Attention", "Attention danger level"));
            dangerLevelRepository.save(new DangerLevelEntity(null, "Alert", "Alert danger level"));
            dangerLevelRepository.save(new DangerLevelEntity(null, "Emergency", "Emergency danger level"));
        }
    }

    private void initAnomalyStatuses() {
        if (statusRepository.count() == 0) {
            statusRepository.save(new AnomalyStatusEntity(null, "Pending", "Anomaly pending analysis"));
            statusRepository.save(new AnomalyStatusEntity(null, "In Progress", "Anomaly in analysis"));
            statusRepository.save(new AnomalyStatusEntity(null, "Completed", "Anomaly resolved"));
            statusRepository.save(new AnomalyStatusEntity(null, "Monitoring", "Anomaly being monitored"));
        }
    }

    public List<AnomalyEntity> findAll() {
        return anomalyRepository.findAll();
    }

    public AnomalyEntity findById(Long id) {
        return anomalyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Anomaly not found!"));
    }

    public List<AnomalyEntity> findByDamId(Long damId) {
        return anomalyRepository.findByDamId(damId);
    }

    @Transactional
    public AnomalyEntity create(AnomalyDTO request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found!"));

        DamEntity dam = damRepository.findById(request.getDamId())
                .orElseThrow(() -> new NotFoundException("Dam not found!"));

        DangerLevelEntity dangerLevel = dangerLevelRepository.findById(request.getDangerLevelId())
                .orElseThrow(() -> new NotFoundException("Danger level not found!"));

        AnomalyStatusEntity status = statusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new NotFoundException("Status not found!"));

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setUser(user);
        anomaly.setDam(dam);
        anomaly.setLatitude(request.getLatitude());
        anomaly.setLongitude(request.getLongitude());
        anomaly.setQuestionnaireId(request.getQuestionnaireId());
        anomaly.setQuestionId(request.getQuestionId());
        anomaly.setOrigin(request.getOrigin());
        anomaly.setObservation(request.getObservation());
        anomaly.setRecommendation(request.getRecommendation());
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);

        if (request.getPhotoBase64() != null && !request.getPhotoBase64().isEmpty()) {
            String photoUrl = processAndSavePhoto(request.getPhotoBase64());
            anomaly.setPhotoPath(photoUrl);
        } else {
            throw new FileStorageException("Photo is required for anomaly!");
        }

        return anomalyRepository.save(anomaly);
    }

    @Transactional
    public AnomalyEntity update(Long id, AnomalyDTO request) {
        AnomalyEntity anomaly = findById(id);

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found!"));

        DamEntity dam = damRepository.findById(request.getDamId())
                .orElseThrow(() -> new NotFoundException("Dam not found!"));

        DangerLevelEntity dangerLevel = dangerLevelRepository.findById(request.getDangerLevelId())
                .orElseThrow(() -> new NotFoundException("Danger level not found!"));

        AnomalyStatusEntity status = statusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new NotFoundException("Status not found!"));

        anomaly.setUser(user);
        anomaly.setDam(dam);
        anomaly.setLatitude(request.getLatitude());
        anomaly.setLongitude(request.getLongitude());
        anomaly.setQuestionnaireId(request.getQuestionnaireId());
        anomaly.setQuestionId(request.getQuestionId());
        anomaly.setOrigin(request.getOrigin());
        anomaly.setObservation(request.getObservation());
        anomaly.setRecommendation(request.getRecommendation());
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);

        if (request.getPhotoBase64() != null && !request.getPhotoBase64().isEmpty()) {
            if (anomaly.getPhotoPath() != null && !anomaly.getPhotoPath().isEmpty()) {
                fileStorageService.deleteFile(anomaly.getPhotoPath());
            }

            String photoUrl = processAndSavePhoto(request.getPhotoBase64());
            anomaly.setPhotoPath(photoUrl);
        }

        return anomalyRepository.save(anomaly);
    }

    @Transactional
    public void delete(Long id) {
        AnomalyEntity anomaly = findById(id);

        if (anomaly.getPhotoPath() != null && !anomaly.getPhotoPath().isEmpty()) {
            fileStorageService.deleteFile(anomaly.getPhotoPath());
        }

        anomalyRepository.delete(anomaly);
    }

    private String processAndSavePhoto(String base64Image) {
        try {
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            return fileStorageService.storeFileFromBytes(
                    imageBytes,
                    "anomaly_photo.jpg",
                    "image/jpeg",
                    "anomalies"
            );
        } catch (IllegalArgumentException e) {
            throw new FileStorageException("Invalid image data provided", e);
        }
    }
}
