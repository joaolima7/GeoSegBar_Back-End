package com.geosegbar.infra.map_kml.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosegbar.common.enums.KmlProcessStatusEnum;
import com.geosegbar.entities.MapKmlFileEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.map_kml.dtos.KmlFeatureDTO;
import com.geosegbar.infra.map_kml.dtos.KmlFeatureUpdateRequest;
import com.geosegbar.infra.map_kml.persistence.jpa.MapKmlFileRepository;
import com.geosegbar.infra.map_kml.processing.KmlParserService;
import com.geosegbar.infra.map_kml.processing.KmlRawFeature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KmlProcessingService {

    private final MapKmlFileRepository fileRepository;
    private final KmlParserService kmlParser;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<KmlFeatureDTO>> FEATURES_TYPE = new TypeReference<>() {};

    // ── Async processing ──────────────────────────────────────────────────────

    @Async
    @Transactional
    public void processFileAsync(Long fileId) {
        MapKmlFileEntity file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
            log.warn("[KML] Arquivo {} não encontrado para processamento.", fileId);
            return;
        }
        log.info("[KML] Iniciando processamento: fileId={}, filename={}", fileId, file.getFilename());
        file.setProcessStatus(KmlProcessStatusEnum.PROCESSING);
        fileRepository.save(file);

        try {
            boolean isKmz = isKmz(file);
            byte[] bytes = fileStorageService.downloadFileBytes(file.getDownloadUrl());
            List<KmlRawFeature> raw = kmlParser.parse(bytes, isKmz);

            List<KmlFeatureDTO> features = raw.stream().map(this::toDTO).collect(Collectors.toList());
            String featuresJson = objectMapper.writeValueAsString(features);

            double minLat = raw.stream().mapToDouble(KmlRawFeature::getMinLat).min().orElse(0);
            double maxLat = raw.stream().mapToDouble(KmlRawFeature::getMaxLat).max().orElse(0);
            double minLng = raw.stream().mapToDouble(KmlRawFeature::getMinLng).min().orElse(0);
            double maxLng = raw.stream().mapToDouble(KmlRawFeature::getMaxLng).max().orElse(0);
            String boundsJson = String.format("{\"minLat\":%s,\"maxLat\":%s,\"minLng\":%s,\"maxLng\":%s}",
                    minLat, maxLat, minLng, maxLng);

            file.setFeaturesJson(featuresJson);
            file.setFeatureCount(features.size());
            file.setBoundsJson(boundsJson);
            file.setProcessStatus(KmlProcessStatusEnum.PROCESSED);
            file.setProcessedAt(LocalDateTime.now());
            fileRepository.save(file);

            log.info("[KML] Processamento concluído: fileId={}, features={}", fileId, features.size());
        } catch (Exception e) {
            log.error("[KML] Falha no processamento fileId={}: {}", fileId, e.getMessage(), e);
            file.setProcessStatus(KmlProcessStatusEnum.FAILED);
            fileRepository.save(file);
        }
    }

    // ── Feature read ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<KmlFeatureDTO> getFeatures(Long fileId, String typeFilter) {
        MapKmlFileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Arquivo KML não encontrado com ID: " + fileId));

        if (file.getFeaturesJson() == null) return List.of();

        List<KmlFeatureDTO> features = deserialize(file.getFeaturesJson());
        if (typeFilter != null && !typeFilter.isBlank()) {
            return features.stream()
                    .filter(f -> typeFilter.equalsIgnoreCase(f.getType()))
                    .collect(Collectors.toList());
        }
        return features;
    }

    // ── Feature update + KML patch ────────────────────────────────────────────

    @Transactional
    public KmlFeatureDTO updateFeature(Long fileId, Integer featureIndex, KmlFeatureUpdateRequest req) {
        if (req.getCustomName() == null && req.getCustomIconClass() == null && req.getCustomColor() == null) {
            throw new InvalidInputException("Nenhuma alteração informada.");
        }

        MapKmlFileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Arquivo KML não encontrado com ID: " + fileId));

        if (file.getFeaturesJson() == null) {
            throw new NotFoundException("Arquivo ainda não foi processado.");
        }

        List<KmlFeatureDTO> features = deserialize(file.getFeaturesJson());

        KmlFeatureDTO feature = features.stream()
                .filter(f -> featureIndex.equals(f.getFeatureIndex()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Feature " + featureIndex + " não encontrada no arquivo."));

        applyOverrides(feature, req);

        file.setFeaturesJson(serialize(features));
        fileRepository.save(file);

        if (req.getCustomName() != null || req.getCustomColor() != null) {
            patchKmlFile(file, feature, req);
        }

        return feature;
    }

    // ── KML/KMZ patch in S3 ───────────────────────────────────────────────────

    private void patchKmlFile(MapKmlFileEntity file, KmlFeatureDTO feature, KmlFeatureUpdateRequest req) {
        boolean isKmz = isKmz(file);
        try {
            byte[] fileBytes = fileStorageService.downloadFileBytes(file.getDownloadUrl());
            byte[] kmlBytes = kmlParser.extractKmlBytes(fileBytes, isKmz);
            Document doc = kmlParser.buildDocument(kmlBytes);

            List<Element> placemarks = kmlParser.collectPlacemarks(doc);
            int idx = feature.getFeatureIndex();
            if (idx >= placemarks.size()) {
                log.warn("[KML] featureIndex {} fora do intervalo ({}) — fileId={}", idx, placemarks.size(), file.getId());
                return;
            }
            kmlParser.applyPlacemarkEdit(placemarks.get(idx), req.getCustomName(), req.getCustomColor(), feature.getType(), doc);

            byte[] newKml = kmlParser.serializeDocument(doc);
            byte[] upload = isKmz ? kmlParser.repackKmz(fileBytes, newKml) : newKml;
            fileStorageService.overwriteFile(file.getDownloadUrl(), upload, file.getContentType());

            log.info("[KML] Arquivo atualizado no S3: fileId={}, featureIndex={}", file.getId(), idx);
        } catch (Exception e) {
            log.error("[KML] Falha ao patchar KML fileId={}: {}", file.getId(), e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void applyOverrides(KmlFeatureDTO f, KmlFeatureUpdateRequest req) {
        if (req.getCustomName() != null) {
            f.setCustomName(req.getCustomName().isBlank() ? null : req.getCustomName());
        }
        if (req.getCustomIconClass() != null) {
            f.setCustomIconClass(req.getCustomIconClass().isBlank() ? null : req.getCustomIconClass());
        }
        if (req.getCustomColor() != null) {
            f.setCustomColor(req.getCustomColor().isBlank() ? null : req.getCustomColor());
        }
    }

    private KmlFeatureDTO toDTO(KmlRawFeature r) {
        KmlFeatureDTO dto = new KmlFeatureDTO();
        dto.setFeatureIndex(r.getFeatureIndex());
        dto.setOriginalName(r.getName());
        dto.setType(r.getType());
        dto.setFolderPath(r.getFolderPath());
        dto.setStyleColor(r.getStyleColor());
        dto.setStrokeColor(r.getStrokeColor());
        dto.setFillColor(r.getFillColor());
        dto.setIconHref(r.getIconHref());
        dto.setMinLat(r.getMinLat());
        dto.setMinLng(r.getMinLng());
        dto.setMaxLat(r.getMaxLat());
        dto.setMaxLng(r.getMaxLng());
        try {
            dto.setGeometry(objectMapper.readTree(r.getGeometryJson()));
        } catch (Exception ignored) {}
        return dto;
    }

    private List<KmlFeatureDTO> deserialize(String json) {
        try {
            return objectMapper.readValue(json, FEATURES_TYPE);
        } catch (Exception e) {
            log.error("[KML] Falha ao desserializar features: {}", e.getMessage());
            return List.of();
        }
    }

    private String serialize(List<KmlFeatureDTO> features) {
        try {
            return objectMapper.writeValueAsString(features);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serializar features KML.", e);
        }
    }

    private boolean isKmz(MapKmlFileEntity file) {
        return "application/vnd.google-earth.kmz".equals(file.getContentType())
                || (file.getFilename() != null && file.getFilename().toLowerCase().endsWith(".kmz"));
    }
}
