package com.geosegbar.infra.map_kml.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.MapKmlFileEntity;
import com.geosegbar.entities.MapKmlFolderEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.map_kml.dtos.MapKmlFileResponseDTO;
import com.geosegbar.infra.map_kml.dtos.MapKmlFolderCreateDTO;
import com.geosegbar.infra.map_kml.dtos.MapKmlFolderResponseDTO;
import com.geosegbar.infra.map_kml.dtos.MapKmlFolderUpdateDTO;
import com.geosegbar.infra.map_kml.persistence.jpa.MapKmlFileRepository;
import com.geosegbar.infra.map_kml.persistence.jpa.MapKmlFolderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MapKmlFolderService {

    private final MapKmlFolderRepository folderRepository;
    private final MapKmlFileRepository fileRepository;
    private final DamRepository damRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<MapKmlFolderResponseDTO> findByDamId(Long damId) {
        if (!damRepository.existsById(damId)) {
            throw new NotFoundException("Barragem não encontrada com ID: " + damId);
        }
        return folderRepository.findByDamId(damId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public MapKmlFolderResponseDTO create(MapKmlFolderCreateDTO dto) {
        checkWritePermission();

        DamEntity dam = damRepository.findById(dto.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + dto.getDamId()));

        if (folderRepository.existsByNameAndDamId(dto.getName(), dto.getDamId())) {
            throw new DuplicateResourceException("Já existe uma pasta com o nome '" + dto.getName() + "' para esta barragem.");
        }

        MapKmlFolderEntity folder = new MapKmlFolderEntity();
        folder.setName(dto.getName());
        folder.setDam(dam);

        MapKmlFolderEntity saved = folderRepository.save(folder);
        log.info("Pasta KML '{}' criada para barragem {}", dto.getName(), dam.getName());
        return toResponseDTO(saved);
    }

    @Transactional
    public MapKmlFolderResponseDTO update(Long id, MapKmlFolderUpdateDTO dto) {
        checkWritePermission();

        MapKmlFolderEntity folder = folderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada com ID: " + id));

        if (folderRepository.existsByNameAndDamIdAndIdNot(dto.getName(), folder.getDam().getId(), id)) {
            throw new DuplicateResourceException("Já existe uma pasta com o nome '" + dto.getName() + "' para esta barragem.");
        }

        folder.setName(dto.getName());
        return toResponseDTO(folderRepository.save(folder));
    }

    @Transactional
    public void delete(Long id) {
        checkWritePermission();

        MapKmlFolderEntity folder = folderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada com ID: " + id));

        folder.getFiles().forEach(file -> {
            try {
                fileStorageService.deleteFile(file.getDownloadUrl());
            } catch (Exception e) {
                log.warn("Falha ao deletar arquivo S3: {}", file.getFilePath());
            }
        });

        folderRepository.delete(folder);
        log.info("Pasta KML '{}' deletada", folder.getName());
    }

    @Transactional
    public MapKmlFolderResponseDTO uploadFile(Long folderId, MultipartFile file) {
        checkWritePermission();

        MapKmlFolderEntity folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada com ID: " + folderId));

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!originalFilename.endsWith(".kml") && !originalFilename.endsWith(".kmz")) {
            throw new InvalidInputException("Apenas arquivos .kml e .kmz são permitidos.");
        }

        String subDirectory = "map-kml/" + folder.getDam().getId();
        String downloadUrl = fileStorageService.storeFile(file, subDirectory);

        String contentType = originalFilename.endsWith(".kmz")
                ? "application/vnd.google-earth.kmz"
                : "application/vnd.google-earth.kml+xml";

        MapKmlFileEntity kmlFile = new MapKmlFileEntity();
        kmlFile.setFilename(file.getOriginalFilename());
        kmlFile.setFilePath(downloadUrl);
        kmlFile.setDownloadUrl(downloadUrl);
        kmlFile.setContentType(contentType);
        kmlFile.setSize(file.getSize());
        kmlFile.setFolder(folder);

        fileRepository.save(kmlFile);
        log.info("Arquivo KML '{}' enviado para pasta '{}'", file.getOriginalFilename(), folder.getName());

        return toResponseDTO(folderRepository.findById(folderId).orElseThrow());
    }

    @Transactional
    public void deleteFile(Long fileId) {
        checkWritePermission();

        MapKmlFileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Arquivo não encontrado com ID: " + fileId));

        try {
            fileStorageService.deleteFile(file.getDownloadUrl());
        } catch (Exception e) {
            log.warn("Falha ao deletar arquivo S3: {}", file.getFilePath());
        }

        fileRepository.delete(file);
        log.info("Arquivo KML '{}' deletado", file.getFilename());
    }

    public MapKmlFolderResponseDTO toResponseDTO(MapKmlFolderEntity folder) {
        List<MapKmlFileResponseDTO> files = folder.getFiles().stream()
                .map(f -> new MapKmlFileResponseDTO(
                        f.getId(), f.getFilename(), f.getDownloadUrl(),
                        f.getContentType(), f.getSize(), f.getUploadedAt()))
                .collect(Collectors.toList());

        return new MapKmlFolderResponseDTO(
                folder.getId(),
                folder.getDam().getId(),
                folder.getName(),
                folder.getCreatedAt(),
                files
        );
    }

    private void checkWritePermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (!user.getAttributionsPermission().getEditDam()) {
                throw new UnauthorizedException("Usuário não tem permissão para gerenciar camadas KML do mapa!");
            }
        }
    }
}
