package com.geosegbar.infra.instrument_graph_pattern_folder.services;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.InstrumentGraphPatternFolder;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.instrument_graph_pattern.persistence.jpa.InstrumentGraphPatternRepository;
import com.geosegbar.infra.instrument_graph_pattern.services.InstrumentGraphPatternService;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.CreateFolderRequestDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.DamFoldersWithPatternsDetailResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.FolderDetailResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.FolderResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.FolderWithPatternsDetailResponseDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.dtos.UpdateFolderRequestDTO;
import com.geosegbar.infra.instrument_graph_pattern_folder.persistence.jpa.InstrumentGraphPatternFolderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentGraphPatternFolderService {

    private final InstrumentGraphPatternFolderRepository folderRepository;
    private final InstrumentGraphPatternRepository patternRepository;
    private final DamService damService;
    private final InstrumentGraphPatternService patternService;

    @Transactional

    public FolderResponseDTO create(CreateFolderRequestDTO request) {
        if (folderRepository.existsByNameAndDamId(request.getName(), request.getDamId())) {
            throw new DuplicateResourceException(
                    "Já existe uma pasta com o nome '" + request.getName() + "' nesta barragem!");
        }

        DamEntity dam = damService.findById(request.getDamId());

        InstrumentGraphPatternFolder folder = new InstrumentGraphPatternFolder();
        folder.setName(request.getName());
        folder.setDam(dam);

        folder = folderRepository.save(folder);

        log.info("Pasta criada: id={}, name={}, damId={}",
                folder.getId(), folder.getName(), dam.getId());

        return mapToResponseDTO(folder);
    }

    @Transactional
    public FolderResponseDTO update(Long folderId, UpdateFolderRequestDTO request) {
        InstrumentGraphPatternFolder folder = findById(folderId);

        boolean nameExists = folderRepository.existsByNameAndDamId(request.getName(), folder.getDam().getId())
                && !folder.getName().equals(request.getName());

        if (nameExists) {
            throw new DuplicateResourceException(
                    "Já existe uma pasta com o nome '" + request.getName() + "' nesta barragem!");
        }

        folder.setName(request.getName());

        if (request.getPatternIds() != null) {
            updatePatternAssociations(folder, request.getPatternIds());
        }

        InstrumentGraphPatternFolder saved = folderRepository.save(folder);

        log.info("Pasta atualizada: id={}, newName={}, patternIds={}",
                saved.getId(), saved.getName(), request.getPatternIds());

        return mapToResponseDTO(saved);
    }

    private void updatePatternAssociations(InstrumentGraphPatternFolder folder, List<Long> newPatternIds) {
        List<InstrumentGraphPatternEntity> currentPatterns = patternRepository.findByFolderId(folder.getId());
        Set<Long> currentPatternIds = currentPatterns.stream()
                .map(InstrumentGraphPatternEntity::getId)
                .collect(Collectors.toSet());

        if (newPatternIds.isEmpty()) {
            removeAllPatternsFromFolder(currentPatterns);
            log.info("Removidos todos {} patterns da pasta {}", currentPatterns.size(), folder.getId());
            return;
        }

        validatePatternsForFolder(folder, newPatternIds);

        Set<Long> newPatternIdsSet = newPatternIds.stream().collect(Collectors.toSet());

        List<Long> patternsToRemove = currentPatternIds.stream()
                .filter(id -> !newPatternIdsSet.contains(id))
                .collect(Collectors.toList());

        List<Long> patternsToAdd = newPatternIdsSet.stream()
                .filter(id -> !currentPatternIds.contains(id))
                .collect(Collectors.toList());

        if (!patternsToRemove.isEmpty()) {
            List<InstrumentGraphPatternEntity> patternsToRemoveEntities = patternRepository.findAllById(patternsToRemove);
            patternsToRemoveEntities.forEach(pattern -> pattern.setFolder(null));
            patternRepository.saveAll(patternsToRemoveEntities);
            log.info("Removidos {} patterns da pasta {}: {}", patternsToRemove.size(), folder.getId(), patternsToRemove);
        }

        if (!patternsToAdd.isEmpty()) {
            List<InstrumentGraphPatternEntity> patternsToAddEntities = patternRepository.findAllById(patternsToAdd);
            patternsToAddEntities.forEach(pattern -> pattern.setFolder(folder));
            patternRepository.saveAll(patternsToAddEntities);
            log.info("Adicionados {} patterns à pasta {}: {}", patternsToAdd.size(), folder.getId(), patternsToAdd);
        }
    }

    private void validatePatternsForFolder(InstrumentGraphPatternFolder folder, List<Long> patternIds) {
        if (patternIds.isEmpty()) {
            return;
        }

        List<InstrumentGraphPatternEntity> patterns = patternRepository.findAllById(patternIds);

        if (patterns.size() != patternIds.size()) {
            Set<Long> foundIds = patterns.stream().map(InstrumentGraphPatternEntity::getId).collect(Collectors.toSet());
            List<Long> notFoundIds = patternIds.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toList());
            throw new NotFoundException("Patterns não encontrados: " + notFoundIds);
        }

        List<Long> invalidPatterns = patterns.stream()
                .filter(pattern -> !pattern.getInstrument().getDam().getId().equals(folder.getDam().getId()))
                .map(InstrumentGraphPatternEntity::getId)
                .collect(Collectors.toList());

        if (!invalidPatterns.isEmpty()) {
            throw new InvalidInputException(
                    "Os patterns " + invalidPatterns + " não pertencem à mesma barragem da pasta (Dam ID: " + folder.getDam().getId() + ")");
        }
    }

    private void removeAllPatternsFromFolder(List<InstrumentGraphPatternEntity> patterns) {
        if (!patterns.isEmpty()) {
            patterns.forEach(pattern -> pattern.setFolder(null));
            patternRepository.saveAll(patterns);
        }
    }

    @Transactional

    public void delete(Long folderId) {
        InstrumentGraphPatternFolder folder = findById(folderId);

        List<InstrumentGraphPatternEntity> patterns = patternRepository.findByFolderId(folderId);
        for (InstrumentGraphPatternEntity pattern : patterns) {
            pattern.setFolder(null);
        }

        if (!patterns.isEmpty()) {
            patternRepository.saveAll(patterns);
            log.info("Removida referência da pasta de {} patterns", patterns.size());
        }

        folderRepository.delete(folder);
        log.info("Pasta excluída: id={}, name={}", folderId, folder.getName());
    }

    @Transactional(readOnly = true)
    public FolderDetailResponseDTO findByIdWithDetails(Long folderId) {
        InstrumentGraphPatternFolder folder = folderRepository.findByIdWithDam(folderId)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada com ID: " + folderId));

        return mapToDetailResponseDTO(folder);
    }

    @Transactional(readOnly = true)
    public FolderResponseDTO findByIdSimple(Long folderId) {
        InstrumentGraphPatternFolder folder = findById(folderId);
        return mapToResponseDTO(folder);
    }

    @Transactional(readOnly = true)
    public InstrumentGraphPatternFolder findById(Long folderId) {

        return folderRepository.findById(folderId)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada com ID: " + folderId));
    }

    @Transactional(readOnly = true)
    public FolderWithPatternsDetailResponseDTO findByIdWithPatternsDetails(Long folderId) {
        InstrumentGraphPatternFolder folder = folderRepository.findByIdWithDam(folderId)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada com ID: " + folderId));

        List<InstrumentGraphPatternEntity> patterns = patternRepository.findByFolderIdWithAllDetails(folderId);

        List<com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternDetailResponseDTO> patternDTOs
                = patterns.stream()
                        .map(patternService::mapToDetailResponseDTO)
                        .collect(Collectors.toList());

        FolderWithPatternsDetailResponseDTO dto = new FolderWithPatternsDetailResponseDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());

        if (folder.getDam() != null) {
            dto.setDam(new FolderWithPatternsDetailResponseDTO.DamDetailDTO(
                    folder.getDam().getId(),
                    folder.getDam().getName(),
                    folder.getDam().getCity(),
                    folder.getDam().getState()
            ));
        }

        dto.setPatterns(patternDTOs);
        return dto;
    }

    @Transactional(readOnly = true)
    public DamFoldersWithPatternsDetailResponseDTO findFoldersWithPatternsDetailsByDam(Long damId) {
        DamEntity dam = damService.findById(damId);

        List<InstrumentGraphPatternFolder> folders = folderRepository.findByDamIdWithDamDetails(damId);

        List<InstrumentGraphPatternEntity> patternsInFolders = patternRepository.findByFolderDamIdWithAllDetails(damId);
        List<InstrumentGraphPatternEntity> patternsWithoutFolder = patternRepository.findByInstrumentDamIdWithoutFolderWithAllDetails(damId);

        Map<Long, List<InstrumentGraphPatternEntity>> patternsByFolder = patternsInFolders.stream()
                .collect(Collectors.groupingBy(p -> p.getFolder().getId()));

        List<FolderWithPatternsDetailResponseDTO> folderDTOs = folders.stream()
                .map(folder -> {
                    List<InstrumentGraphPatternEntity> folderPatterns = patternsByFolder.getOrDefault(folder.getId(), List.of());

                    List<com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternDetailResponseDTO> patternDTOs
                            = folderPatterns.stream()
                                    .map(patternService::mapToDetailResponseDTO)
                                    .collect(Collectors.toList());

                    FolderWithPatternsDetailResponseDTO folderDTO = new FolderWithPatternsDetailResponseDTO();
                    folderDTO.setId(folder.getId());
                    folderDTO.setName(folder.getName());

                    if (folder.getDam() != null) {
                        folderDTO.setDam(new FolderWithPatternsDetailResponseDTO.DamDetailDTO(
                                folder.getDam().getId(),
                                folder.getDam().getName(),
                                folder.getDam().getCity(),
                                folder.getDam().getState()
                        ));
                    }

                    folderDTO.setPatterns(patternDTOs);
                    return folderDTO;
                })
                .collect(Collectors.toList());

        List<com.geosegbar.infra.instrument_graph_pattern.dtos.GraphPatternDetailResponseDTO> patternsWithoutFolderDTOs
                = patternsWithoutFolder.stream()
                        .map(patternService::mapToDetailResponseDTO)
                        .collect(Collectors.toList());

        DamFoldersWithPatternsDetailResponseDTO responseDTO = new DamFoldersWithPatternsDetailResponseDTO();
        responseDTO.setDamId(dam.getId());
        responseDTO.setDamName(dam.getName());
        responseDTO.setDamCity(dam.getCity());
        responseDTO.setDamState(dam.getState());
        responseDTO.setFolders(folderDTOs);
        responseDTO.setPatternsWithoutFolder(patternsWithoutFolderDTOs);

        return responseDTO;
    }

    private FolderResponseDTO mapToResponseDTO(InstrumentGraphPatternFolder folder) {
        FolderResponseDTO dto = new FolderResponseDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());

        if (folder.getDam() != null) {
            dto.setDam(new FolderResponseDTO.DamSummaryDTO(
                    folder.getDam().getId(),
                    folder.getDam().getName()
            ));
        }
        return dto;
    }

    private FolderDetailResponseDTO mapToDetailResponseDTO(InstrumentGraphPatternFolder folder) {
        FolderDetailResponseDTO dto = new FolderDetailResponseDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());

        if (folder.getDam() != null) {
            dto.setDam(new FolderDetailResponseDTO.DamDetailDTO(
                    folder.getDam().getId(),
                    folder.getDam().getName(),
                    folder.getDam().getCity(),
                    folder.getDam().getState()
            ));
        }

        List<InstrumentGraphPatternEntity> patterns = patternRepository.findByFolderId(folder.getId());

        List<FolderDetailResponseDTO.PatternSummaryDTO> patternDTOs = patterns.stream()
                .map(pattern -> {
                    FolderDetailResponseDTO.InstrumentSummaryDTO instrumentDTO = null;
                    if (pattern.getInstrument() != null) {
                        instrumentDTO = new FolderDetailResponseDTO.InstrumentSummaryDTO(
                                pattern.getInstrument().getId(),
                                pattern.getInstrument().getName(),
                                pattern.getInstrument().getLocation()
                        );
                    }
                    return new FolderDetailResponseDTO.PatternSummaryDTO(
                            pattern.getId(),
                            pattern.getName(),
                            instrumentDTO
                    );
                })
                .collect(Collectors.toList());

        dto.setPatterns(patternDTOs);
        return dto;
    }
}
