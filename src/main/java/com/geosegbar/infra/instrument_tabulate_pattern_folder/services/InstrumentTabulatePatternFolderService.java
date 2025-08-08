package com.geosegbar.infra.instrument_tabulate_pattern_folder.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.InstrumentTabulatePatternMapper;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentTabulatePatternEntity;
import com.geosegbar.entities.InstrumentTabulatePatternFolder;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.instrument_tabulate_pattern.dtos.TabulatePatternResponseDTO;
import com.geosegbar.infra.instrument_tabulate_pattern.persistence.jpa.InstrumentTabulatePatternRepository;
import com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos.CreateTabulateFolderRequestDTO;
import com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos.DamTabulateFoldersWithPatternsDetailResponseDTO;
import com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos.TabulateFolderResponseDTO;
import com.geosegbar.infra.instrument_tabulate_pattern_folder.dtos.TabulateFolderWithPatternsDetailResponseDTO;
import com.geosegbar.infra.instrument_tabulate_pattern_folder.persistence.jpa.InstrumentTabulatePatternFolderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentTabulatePatternFolderService {

    private final InstrumentTabulatePatternFolderRepository folderRepository;
    private final InstrumentTabulatePatternRepository patternRepository;
    private final DamService damService;
    private final InstrumentTabulatePatternMapper mapper;

    @Transactional
    public TabulateFolderResponseDTO create(CreateTabulateFolderRequestDTO request) {

        if (folderRepository.existsByNameAndDamId(request.getName(), request.getDamId())) {
            throw new DuplicateResourceException(
                    "Já existe uma pasta de padrões de tabela com o nome '" + request.getName() + "' nesta barragem!");
        }

        DamEntity dam = damService.findById(request.getDamId());

        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setName(request.getName());
        folder.setDam(dam);

        folder = folderRepository.save(folder);

        log.info("Pasta de padrões de tabela criada: id={}, name={}, damId={}",
                folder.getId(), folder.getName(), dam.getId());

        return mapToResponseDTO(folder);
    }

    @Transactional(readOnly = true)
    public DamTabulateFoldersWithPatternsDetailResponseDTO findFoldersWithPatternsDetailsByDam(Long damId) {

        DamEntity dam = damService.findById(damId);

        List<InstrumentTabulatePatternFolder> folders = folderRepository.findByDamIdWithDamDetails(damId);

        List<InstrumentTabulatePatternEntity> patternsInFolders = patternRepository.findByFolderDamIdWithAllDetails(damId);

        List<InstrumentTabulatePatternEntity> patternsWithoutFolder = patternRepository.findByDamIdWithoutFolderWithAllDetails(damId);

        Map<Long, List<InstrumentTabulatePatternEntity>> patternsByFolder = patternsInFolders.stream()
                .collect(Collectors.groupingBy(p -> p.getFolder().getId()));

        List<TabulateFolderWithPatternsDetailResponseDTO> folderDTOs = folders.stream()
                .map(folder -> {

                    List<InstrumentTabulatePatternEntity> folderPatterns = patternsByFolder.getOrDefault(folder.getId(), List.of());

                    List<TabulatePatternResponseDTO> patternDTOs = folderPatterns.stream()
                            .map(mapper::mapToResponseDTO)
                            .collect(Collectors.toList());

                    TabulateFolderWithPatternsDetailResponseDTO folderDTO = new TabulateFolderWithPatternsDetailResponseDTO();
                    folderDTO.setId(folder.getId());
                    folderDTO.setName(folder.getName());

                    if (folder.getDam() != null) {
                        folderDTO.setDam(new TabulateFolderWithPatternsDetailResponseDTO.DamDetailDTO(
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

        List<TabulatePatternResponseDTO> patternsWithoutFolderDTOs = patternsWithoutFolder.stream()
                .map(mapper::mapToResponseDTO)
                .collect(Collectors.toList());

        DamTabulateFoldersWithPatternsDetailResponseDTO responseDTO = new DamTabulateFoldersWithPatternsDetailResponseDTO();
        responseDTO.setDamId(dam.getId());
        responseDTO.setDamName(dam.getName());
        responseDTO.setDamCity(dam.getCity());
        responseDTO.setDamState(dam.getState());
        responseDTO.setFolders(folderDTOs);
        responseDTO.setPatternsWithoutFolder(patternsWithoutFolderDTOs);

        int totalPatternsInFolders = folderDTOs.stream()
                .mapToInt(f -> f.getPatterns().size())
                .sum();

        log.debug("Dam {} - Folders: {}, Patterns em folders: {}, Patterns sem folder: {}, Total patterns: {}",
                damId, folderDTOs.size(), totalPatternsInFolders, patternsWithoutFolderDTOs.size(),
                totalPatternsInFolders + patternsWithoutFolderDTOs.size());

        return responseDTO;
    }

    @Transactional
    public void delete(Long folderId) {
        InstrumentTabulatePatternFolder folder = findById(folderId);

        List<InstrumentTabulatePatternEntity> patterns = patternRepository.findByFolderId(folderId);

        for (InstrumentTabulatePatternEntity pattern : patterns) {
            pattern.setFolder(null);
        }

        if (!patterns.isEmpty()) {
            patternRepository.saveAll(patterns);
            log.info("Removida referência da pasta de {} padrões de tabela", patterns.size());
        }

        folderRepository.delete(folder);

        log.info("Pasta de padrões de tabela excluída: id={}, name={}, padrões afetados={}",
                folderId, folder.getName(), patterns.size());
    }

    public TabulateFolderResponseDTO findByIdSimple(Long folderId) {
        InstrumentTabulatePatternFolder folder = findById(folderId);
        return mapToResponseDTO(folder);
    }

    public TabulateFolderResponseDTO updateFolderName(Long folderId, String newName) {
        InstrumentTabulatePatternFolder folder = findById(folderId);

        if (folderRepository.existsByNameAndDamId(newName, folder.getDam().getId())) {
            throw new DuplicateResourceException(
                    "Já existe uma pasta de padrões de tabela com o nome '" + newName + "' nesta barragem!");
        }

        folder.setName(newName);
        folder = folderRepository.save(folder);

        return mapToResponseDTO(folder);
    }

    public InstrumentTabulatePatternFolder findById(Long folderId) {
        return folderRepository.findById(folderId)
                .orElseThrow(() -> new NotFoundException("Pasta de padrões de tabela não encontrada com ID: " + folderId));
    }

    public List<TabulateFolderResponseDTO> findByDamId(Long damId) {

        damService.findById(damId);

        List<InstrumentTabulatePatternFolder> folders = folderRepository.findByDamIdWithDamDetails(damId);

        return folders.stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    private TabulateFolderResponseDTO mapToResponseDTO(InstrumentTabulatePatternFolder folder) {
        TabulateFolderResponseDTO dto = new TabulateFolderResponseDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());

        if (folder.getDam() != null) {
            dto.setDam(new TabulateFolderResponseDTO.DamSummaryDTO(
                    folder.getDam().getId(),
                    folder.getDam().getName()
            ));
        }

        return dto;
    }
}
