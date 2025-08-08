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
        // Verificar se já existe uma pasta com o mesmo nome na barragem
        if (folderRepository.existsByNameAndDamId(request.getName(), request.getDamId())) {
            throw new DuplicateResourceException(
                    "Já existe uma pasta de padrões de tabela com o nome '" + request.getName() + "' nesta barragem!");
        }

        // Verificar se a barragem existe
        DamEntity dam = damService.findById(request.getDamId());

        // Criar a pasta
        InstrumentTabulatePatternFolder folder = new InstrumentTabulatePatternFolder();
        folder.setName(request.getName());
        folder.setDam(dam);

        folder = folderRepository.save(folder);

        log.info("Pasta de padrões de tabela criada: id={}, name={}, damId={}",
                folder.getId(), folder.getName(), dam.getId());

        return mapToResponseDTO(folder);
    }

    // Adicione este método ao InstrumentTabulatePatternFolderService
    @Transactional(readOnly = true)
    public DamTabulateFoldersWithPatternsDetailResponseDTO findFoldersWithPatternsDetailsByDam(Long damId) {
        // Verificar se a barragem existe
        DamEntity dam = damService.findById(damId);

        // 1. Buscar todas as pastas da barragem (mesmo as vazias)
        List<InstrumentTabulatePatternFolder> folders = folderRepository.findByDamIdWithDamDetails(damId);

        // 2. Buscar patterns que estão em pastas (com todos os detalhes)
        List<InstrumentTabulatePatternEntity> patternsInFolders = patternRepository.findByFolderDamIdWithAllDetails(damId);

        // 3. Buscar patterns que NÃO estão em pastas (com todos os detalhes)
        List<InstrumentTabulatePatternEntity> patternsWithoutFolder = patternRepository.findByDamIdWithoutFolderWithAllDetails(damId);

        // Agrupar patterns por folder ID para performance
        Map<Long, List<InstrumentTabulatePatternEntity>> patternsByFolder = patternsInFolders.stream()
                .collect(Collectors.groupingBy(p -> p.getFolder().getId()));

        // Mapear pastas para DTOs (incluindo pastas vazias)
        List<TabulateFolderWithPatternsDetailResponseDTO> folderDTOs = folders.stream()
                .map(folder -> {
                    // Buscar patterns desta pasta (pode ser lista vazia)
                    List<InstrumentTabulatePatternEntity> folderPatterns = patternsByFolder.getOrDefault(folder.getId(), List.of());

                    // Converter patterns para DTOs
                    List<TabulatePatternResponseDTO> patternDTOs = folderPatterns.stream()
                            .map(mapper::mapToResponseDTO)
                            .collect(Collectors.toList());

                    // Criar DTO da pasta
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

        // Mapear patterns sem pasta
        List<TabulatePatternResponseDTO> patternsWithoutFolderDTOs = patternsWithoutFolder.stream()
                .map(mapper::mapToResponseDTO)
                .collect(Collectors.toList());

        // Criar resposta final
        DamTabulateFoldersWithPatternsDetailResponseDTO responseDTO = new DamTabulateFoldersWithPatternsDetailResponseDTO();
        responseDTO.setDamId(dam.getId());
        responseDTO.setDamName(dam.getName());
        responseDTO.setDamCity(dam.getCity());
        responseDTO.setDamState(dam.getState());
        responseDTO.setFolders(folderDTOs);
        responseDTO.setPatternsWithoutFolder(patternsWithoutFolderDTOs);

        // Logs para debug
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

        // Buscar todos os padrões associados à pasta
        List<InstrumentTabulatePatternEntity> patterns = patternRepository.findByFolderId(folderId);

        // Remover a referência da pasta dos padrões (não excluir os padrões)
        for (InstrumentTabulatePatternEntity pattern : patterns) {
            pattern.setFolder(null);
        }

        // Salvar as alterações nos padrões
        if (!patterns.isEmpty()) {
            patternRepository.saveAll(patterns);
            log.info("Removida referência da pasta de {} padrões de tabela", patterns.size());
        }

        // Excluir a pasta
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
        // Verificar se a barragem existe
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
