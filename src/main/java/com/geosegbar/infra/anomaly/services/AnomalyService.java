package com.geosegbar.infra.anomaly.services;

import java.util.Map;
import java.util.stream.Collectors;
import com.geosegbar.infra.anomaly.dtos.AnomalyListItemDTO;
import com.geosegbar.infra.dashboard.projections.AnomalyPhotoPathProjection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.geosegbar.infra.anomaly.dtos.PagedAnomalyDTO;
import com.geosegbar.infra.dam.services.DamAccessService;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyPhotoEntity;
import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.anomaly.dtos.AnomalyDTO;
import com.geosegbar.infra.anomaly.dtos.UpdateAnomalyRequestDTO;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.anomaly_photo.persistence.jpa.AnomalyPhotoRepository;
import com.geosegbar.infra.anomaly_status.persistence.jpa.AnomalyStatusRepository;
import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;
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
    private final AnomalyPhotoRepository anomalyPhotoRepository;
    private final DamAccessService damAccessService;

    @PostConstruct
    public void init() {
        initDangerLevels();
        initAnomalyStatuses();
    }

    private void initDangerLevels() {
        if (dangerLevelRepository.count() == 0) {
            dangerLevelRepository.save(new DangerLevelEntity(null, "Normal", "Nível de perigo normal"));
            dangerLevelRepository.save(new DangerLevelEntity(null, "Atenção", "Nível de perigo atenção"));
            dangerLevelRepository.save(new DangerLevelEntity(null, "Alerta", "Nível de perigo alerta"));
            dangerLevelRepository.save(new DangerLevelEntity(null, "Emergência", "Nível de perigo emergência"));
        }
    }

    private void initAnomalyStatuses() {
        if (statusRepository.count() == 0) {
            statusRepository.save(new AnomalyStatusEntity(null, "Pendente", "Anomalia pendente"));
            statusRepository.save(new AnomalyStatusEntity(null, "Em andamento", "Anomalia em andamento"));
            statusRepository.save(new AnomalyStatusEntity(null, "Concluído", "Anomalia concluída"));
            statusRepository.save(new AnomalyStatusEntity(null, "Em monitoramento", "Anomalia em monitoramento"));
        }
    }

    /**
     * Listagem filtrada e paginada, recortada pelas barragens que o usuário
     * pode ver.
     *
     * damIds ausente significa "todas as acessíveis"; damIds preenchido
     * intersecta com as acessíveis em vez de lançar 403, porque o app trabalha
     * com permissão cacheada e a defasagem é normal.
     */
    @Transactional(readOnly = true)
    public PagedAnomalyDTO<AnomalyListItemDTO> findByFilters(
            List<Long> damIds, Long statusId,
            LocalDateTime startDate, LocalDateTime endDate,
            int page, int size) {

        List<Long> escopo = new ArrayList<>(damAccessService.intersectWithAccessible(damIds));

        if (escopo.isEmpty()) {
            return new PagedAnomalyDTO<>(List.of(), page, size, 0L, 0, true, true);
        }

        Page<AnomalyEntity> resultado = anomalyRepository.findByFilters(
                escopo, statusId, startDate, endDate,
                PageRequest.of(page, size));

        // As fotos vêm numa segunda consulta, em lote, só para os ids desta
        // página. Trazê-las junto no EntityGraph faria o Hibernate paginar em
        // memória, que é justamente o que esta rota existe para evitar.
        Map<Long, List<String>> fotosPorAnomalia = fotosDe(resultado.getContent());

        List<AnomalyListItemDTO> conteudo = resultado.getContent().stream()
                .map(a -> toListItem(a, fotosPorAnomalia.getOrDefault(a.getId(), List.of())))
                .toList();

        return new PagedAnomalyDTO<>(
                conteudo,
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                resultado.isLast(),
                resultado.isFirst());
    }

    private Map<Long, List<String>> fotosDe(List<AnomalyEntity> anomalias) {
        if (anomalias.isEmpty()) {
            return Map.of();
        }

        List<Long> ids = anomalias.stream().map(AnomalyEntity::getId).toList();

        return anomalyPhotoRepository.findPathsByAnomalyIds(ids).stream()
                .collect(Collectors.groupingBy(
                        AnomalyPhotoPathProjection::getAnomalyId,
                        Collectors.mapping(AnomalyPhotoPathProjection::getImagePath, Collectors.toList())));
    }

    private AnomalyListItemDTO toListItem(AnomalyEntity a, List<String> fotos) {
        return new AnomalyListItemDTO(
                a.getId(),
                a.getCreatedAt(),
                a.getDam() != null ? a.getDam().getId() : null,
                a.getDam() != null ? a.getDam().getName() : null,
                a.getUser() != null ? a.getUser().getId() : null,
                a.getUser() != null ? a.getUser().getName() : null,
                a.getLatitude(),
                a.getLongitude(),
                a.getOrigin() != null ? a.getOrigin().name() : null,
                a.getObservation(),
                a.getRecommendation(),
                a.getDangerLevel() != null ? a.getDangerLevel().getId() : null,
                a.getDangerLevel() != null ? a.getDangerLevel().getName() : null,
                a.getStatus() != null ? a.getStatus().getId() : null,
                a.getStatus() != null ? a.getStatus().getName() : null,
                a.getQuestionnaireId(),
                a.getQuestionId(),
                fotos);
    }

    @Transactional(readOnly = true)
    public List<AnomalyEntity> findAll() {
        return anomalyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AnomalyEntity findById(Long id) {
        return anomalyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Anomalia não encontrada!"));
    }

    @Transactional(readOnly = true)
    public List<AnomalyEntity> findByDamId(Long damId) {
        return anomalyRepository.findByDamId(damId);
    }

    @Transactional
    public AnomalyEntity create(AnomalyDTO request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        DamEntity dam = damRepository.findById(request.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));

        DangerLevelEntity dangerLevel = dangerLevelRepository.findById(request.getDangerLevelId())
                .orElseThrow(() -> new NotFoundException("Nível de perigo não encontrado!"));

        AnomalyStatusEntity status = statusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new NotFoundException("Status não encontrado!"));

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

        AnomalyEntity savedAnomaly = anomalyRepository.save(anomaly);

        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            for (PhotoSubmissionDTO photoDto : request.getPhotos()) {
                saveAnomalyPhoto(photoDto, savedAnomaly, dam.getId());
            }
        }

        return findById(savedAnomaly.getId());
    }

    @Transactional
    public AnomalyEntity update(Long id, UpdateAnomalyRequestDTO request) {
        AnomalyEntity anomaly = findById(id);

        boolean hasAnyField = request.getObservation() != null
                || request.getRecommendation() != null
                || request.getDangerLevelId() != null
                || request.getStatusId() != null;

        if (!hasAnyField) {
            throw new InvalidInputException("Informe ao menos um campo para atualização: Observação, Recomendação, Nível de Perigo ou Status.");
        }

        if (request.getObservation() != null) {
            anomaly.setObservation(request.getObservation());
        }

        if (request.getRecommendation() != null) {
            anomaly.setRecommendation(request.getRecommendation());
        }

        if (request.getDangerLevelId() != null) {
            DangerLevelEntity dangerLevel = dangerLevelRepository.findById(request.getDangerLevelId())
                    .orElseThrow(() -> new NotFoundException("Nível de perigo não encontrado!"));
            anomaly.setDangerLevel(dangerLevel);
        }

        if (request.getStatusId() != null) {
            AnomalyStatusEntity status = statusRepository.findById(request.getStatusId())
                    .orElseThrow(() -> new NotFoundException("Status não encontrado!"));
            anomaly.setStatus(status);
        }

        AnomalyEntity saved = anomalyRepository.save(anomaly);
        return findById(saved.getId());
    }

    @Transactional
    public void delete(Long id) {
        AnomalyEntity anomaly = findById(id);

        if (anomaly.getPhotos() != null) {
            for (AnomalyPhotoEntity photo : anomaly.getPhotos()) {
                if (photo.getImagePath() != null && !photo.getImagePath().isEmpty()) {
                    fileStorageService.deleteFile(photo.getImagePath());
                }
            }
        }

        anomalyRepository.delete(anomaly);
    }

    private AnomalyPhotoEntity saveAnomalyPhoto(PhotoSubmissionDTO photoDto, AnomalyEntity anomaly, Long damId) {
        try {
            String base64Image = photoDto.getBase64Image();
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            String photoUrl = fileStorageService.storeFileFromBytes(
                    imageBytes,
                    photoDto.getFileName(),
                    photoDto.getContentType(),
                    "anomalies"
            );

            AnomalyPhotoEntity photoEntity = new AnomalyPhotoEntity();
            photoEntity.setAnomaly(anomaly);
            photoEntity.setImagePath(photoUrl);
            photoEntity.setDamId(damId);
            return anomalyPhotoRepository.save(photoEntity);
        } catch (Exception e) {
            throw new FileStorageException("Erro ao processar imagem da anomalia: " + e.getMessage());
        }
    }
}
