package com.geosegbar.infra.instrument_tabulate_pattern.services;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.InstrumentTabulatePatternMapper;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTabulateAssociationEntity;
import com.geosegbar.entities.InstrumentTabulateOutputAssociationEntity;
import com.geosegbar.entities.InstrumentTabulatePatternEntity;
import com.geosegbar.entities.InstrumentTabulatePatternFolder;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.instrument.services.InstrumentService;
import com.geosegbar.infra.instrument_tabulate_pattern.dtos.CreateTabulatePatternRequestDTO;
import com.geosegbar.infra.instrument_tabulate_pattern.dtos.TabulatePatternResponseDTO;
import com.geosegbar.infra.instrument_tabulate_pattern.dtos.UpdateTabulatePatternRequestDTO;
import com.geosegbar.infra.instrument_tabulate_pattern.persistence.jpa.InstrumentTabulatePatternRepository;
import com.geosegbar.infra.instrument_tabulate_pattern_folder.services.InstrumentTabulatePatternFolderService;
import com.geosegbar.infra.output.services.OutputService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentTabulatePatternService {

    private final InstrumentTabulatePatternRepository patternRepository;
    private final DamService damService;
    private final InstrumentService instrumentService;
    private final OutputService outputService;
    private final InstrumentTabulatePatternFolderService folderService;
    private final InstrumentTabulatePatternMapper mapper;

    @Transactional
    @CacheEvict(value = {"tabulatePatterns", "tabulatePatternsByDam", "tabulatePatternsByFolder",
        "tabulateFolderWithPatterns", "damTabulateFoldersWithPatterns"},
            allEntries = true, cacheManager = "instrumentTabulateCacheManager")
    public TabulatePatternResponseDTO create(CreateTabulatePatternRequestDTO request) {

        if (patternRepository.existsByNameAndDamId(request.getName(), request.getDamId())) {
            throw new DuplicateResourceException(
                    "Já existe um padrão de tabela com o nome '" + request.getName() + "' nesta barragem!");
        }

        DamEntity dam = damService.findById(request.getDamId());

        InstrumentTabulatePatternFolder folder = null;
        if (request.getFolderId() != null) {
            folder = folderService.findById(request.getFolderId());

            if (!folder.getDam().getId().equals(dam.getId())) {
                throw new InvalidInputException(
                        "A pasta selecionada não pertence à barragem informada!");
            }
        }

        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setName(request.getName());
        pattern.setDam(dam);
        pattern.setFolder(folder);
        pattern.setIsLinimetricRulerEnable(request.getIsLinimetricRulerEnable());

        pattern = patternRepository.save(pattern);

        Set<InstrumentTabulateAssociationEntity> associations = new HashSet<>();
        validateAndProcessInstrumentAssociations(request.getAssociations(), pattern, associations);

        pattern.setAssociations(associations);

        pattern = patternRepository.save(pattern);

        log.info("Padrão de tabela criado: id={}, name={}, damId={}, folderId={}",
                pattern.getId(), pattern.getName(), dam.getId(),
                folder != null ? folder.getId() : "sem pasta");

        return mapper.mapToResponseDTO(patternRepository.findByIdWithAllDetails(pattern.getId())
                .orElseThrow(() -> new NotFoundException("Padrão de tabela não encontrado após criação")));
    }

    @Transactional
    @CacheEvict(value = {"tabulatePatterns", "tabulatePatternsByDam", "tabulatePatternsByFolder",
        "tabulateFolderWithPatterns", "damTabulateFoldersWithPatterns"},
            allEntries = true, cacheManager = "instrumentTabulateCacheManager")
    public void delete(Long patternId) {
        InstrumentTabulatePatternEntity pattern = patternRepository.findByIdWithBasicDetails(patternId)
                .orElseThrow(() -> new NotFoundException("Padrão de tabela não encontrado com ID: " + patternId));

        log.info("Excluindo padrão de tabela: id={}, name={}, damId={}, folderId={}",
                pattern.getId(), pattern.getName(),
                pattern.getDam() != null ? pattern.getDam().getId() : null,
                pattern.getFolder() != null ? pattern.getFolder().getId() : null);

        patternRepository.delete(pattern);

        log.info("Padrão de tabela excluído com sucesso: id={}", patternId);
    }

    @Transactional
    @CacheEvict(value = {"tabulatePatterns", "tabulatePatternsByDam", "tabulatePatternsByFolder",
        "tabulateFolderWithPatterns", "damTabulateFoldersWithPatterns"},
            allEntries = true, cacheManager = "instrumentTabulateCacheManager")
    public TabulatePatternResponseDTO update(Long patternId, UpdateTabulatePatternRequestDTO request) {

        InstrumentTabulatePatternEntity pattern = findEntityByIdWithAllDetails(patternId);

        if (!pattern.getName().equals(request.getName())
                && patternRepository.existsByNameAndDamIdAndIdNot(request.getName(), pattern.getDam().getId(), patternId)) {
            throw new DuplicateResourceException(
                    "Já existe um padrão de tabela com o nome '" + request.getName() + "' nesta barragem!");
        }

        InstrumentTabulatePatternFolder folder = null;
        if (request.getFolderId() != null) {
            folder = folderService.findById(request.getFolderId());

            if (!folder.getDam().getId().equals(pattern.getDam().getId())) {
                throw new InvalidInputException(
                        "A pasta selecionada não pertence à barragem do padrão!");
            }
        }

        pattern.setName(request.getName());
        pattern.setFolder(folder);
        pattern.setIsLinimetricRulerEnable(request.getIsLinimetricRulerEnable());

        Set<InstrumentTabulateAssociationEntity> currentAssociations = pattern.getAssociations();
        Set<InstrumentTabulateAssociationEntity> newAssociations = new HashSet<>();

        Map<Long, InstrumentTabulateAssociationEntity> associationsMap = currentAssociations.stream()
                .collect(Collectors.toMap(InstrumentTabulateAssociationEntity::getId, a -> a));

        validateAndUpdateInstrumentAssociations(request.getAssociations(), pattern, newAssociations, associationsMap);

        pattern.getAssociations().clear();
        pattern.getAssociations().addAll(newAssociations);

        pattern = patternRepository.save(pattern);

        log.info("Padrão de tabela atualizado: id={}, name={}, damId={}, folderId={}",
                pattern.getId(), pattern.getName(), pattern.getDam().getId(),
                folder != null ? folder.getId() : "sem pasta");

        return mapper.mapToResponseDTO(patternRepository.findByIdWithAllDetails(pattern.getId())
                .orElseThrow(() -> new NotFoundException("Padrão de tabela não encontrado após atualização")));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tabulatePatterns", key = "#patternId", cacheManager = "instrumentTabulateCacheManager")
    public TabulatePatternResponseDTO findById(Long patternId) {
        InstrumentTabulatePatternEntity pattern = findEntityByIdWithAllDetails(patternId);
        return mapper.mapToResponseDTO(pattern);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tabulatePatternsByDam", key = "#damId", cacheManager = "instrumentTabulateCacheManager")
    public List<TabulatePatternResponseDTO> findByDamId(Long damId) {

        damService.findById(damId);

        List<InstrumentTabulatePatternEntity> patterns = patternRepository.findByDamIdWithAllDetails(damId);
        return patterns.stream()
                .map(mapper::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tabulatePatternsByFolder", key = "#folderId", cacheManager = "instrumentTabulateCacheManager")
    public List<TabulatePatternResponseDTO> findByFolderId(Long folderId) {

        folderService.findById(folderId);

        List<InstrumentTabulatePatternEntity> patterns = patternRepository.findByFolderIdWithAllDetails(folderId);
        return patterns.stream()
                .map(mapper::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public InstrumentTabulatePatternEntity findEntityByIdWithAllDetails(Long patternId) {
        return patternRepository.findByIdWithAllDetails(patternId)
                .orElseThrow(() -> new NotFoundException("Padrão de tabela não encontrado com ID: " + patternId));
    }

    private void validateAndProcessInstrumentAssociations(
            List<CreateTabulatePatternRequestDTO.InstrumentAssociationDTO> associationDTOs,
            InstrumentTabulatePatternEntity pattern,
            Set<InstrumentTabulateAssociationEntity> associations) {

        Set<Integer> usedIndices = new HashSet<>();
        int nextAvailableIndex = 1;

        for (CreateTabulatePatternRequestDTO.InstrumentAssociationDTO dto : associationDTOs) {

            InstrumentEntity instrument = instrumentService.findById(dto.getInstrumentId());
            if (!instrument.getDam().getId().equals(pattern.getDam().getId())) {
                throw new InvalidInputException(
                        "O instrumento com ID " + dto.getInstrumentId() + " não pertence à barragem do padrão!");
            }

            InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
            association.setPattern(pattern);
            association.setInstrument(instrument);

            configureBaseColumns(dto, association, usedIndices, nextAvailableIndex);
            nextAvailableIndex = getNextAvailableIndex(usedIndices);

            processOutputAssociations(dto.getOutputAssociations(), association, instrument, usedIndices, nextAvailableIndex);
            nextAvailableIndex = getNextAvailableIndex(usedIndices);

            associations.add(association);
        }
    }

    private void validateAndUpdateInstrumentAssociations(
            List<UpdateTabulatePatternRequestDTO.InstrumentAssociationDTO> associationDTOs,
            InstrumentTabulatePatternEntity pattern,
            Set<InstrumentTabulateAssociationEntity> newAssociations,
            Map<Long, InstrumentTabulateAssociationEntity> existingAssociationsMap) {

        Set<Integer> usedIndices = new HashSet<>();
        int nextAvailableIndex = 1;

        for (UpdateTabulatePatternRequestDTO.InstrumentAssociationDTO dto : associationDTOs) {
            InstrumentTabulateAssociationEntity association;

            if (dto.getId() != null && existingAssociationsMap.containsKey(dto.getId())) {

                association = existingAssociationsMap.get(dto.getId());

                if (!association.getInstrument().getId().equals(dto.getInstrumentId())) {

                    InstrumentEntity newInstrument = instrumentService.findById(dto.getInstrumentId());
                    if (!newInstrument.getDam().getId().equals(pattern.getDam().getId())) {
                        throw new InvalidInputException(
                                "O instrumento com ID " + dto.getInstrumentId() + " não pertence à barragem do padrão!");
                    }
                    association.setInstrument(newInstrument);
                }

                association.getOutputAssociations().clear();
            } else {

                association = new InstrumentTabulateAssociationEntity();
                association.setPattern(pattern);

                InstrumentEntity instrument = instrumentService.findById(dto.getInstrumentId());
                if (!instrument.getDam().getId().equals(pattern.getDam().getId())) {
                    throw new InvalidInputException(
                            "O instrumento com ID " + dto.getInstrumentId() + " não pertence à barragem do padrão!");
                }
                association.setInstrument(instrument);
            }

            configureBaseColumnsForUpdate(dto, association, usedIndices, nextAvailableIndex);
            nextAvailableIndex = getNextAvailableIndex(usedIndices);

            processOutputAssociationsForUpdate(dto.getOutputAssociations(), association, association.getInstrument(), usedIndices, nextAvailableIndex);
            nextAvailableIndex = getNextAvailableIndex(usedIndices);

            newAssociations.add(association);
        }
    }

    private void configureBaseColumns(
            CreateTabulatePatternRequestDTO.InstrumentAssociationDTO dto,
            InstrumentTabulateAssociationEntity association,
            Set<Integer> usedIndices,
            int nextAvailableIndex) {

        if (Boolean.TRUE.equals(dto.getIsDateEnable())) {
            association.setIsDateEnable(true);
            if (dto.getDateIndex() != null) {
                if (usedIndices.contains(dto.getDateIndex())) {
                    throw new InvalidInputException("Índice duplicado: " + dto.getDateIndex());
                }
                association.setDateIndex(dto.getDateIndex());
                usedIndices.add(dto.getDateIndex());
            } else {
                association.setDateIndex(nextAvailableIndex);
                usedIndices.add(nextAvailableIndex);
                nextAvailableIndex++;
            }
        } else {
            association.setIsDateEnable(false);
            association.setDateIndex(null);
        }

        if (Boolean.TRUE.equals(dto.getIsHourEnable())) {
            association.setIsHourEnable(true);
            if (dto.getHourIndex() != null) {
                if (usedIndices.contains(dto.getHourIndex())) {
                    throw new InvalidInputException("Índice duplicado: " + dto.getHourIndex());
                }
                association.setHourIndex(dto.getHourIndex());
                usedIndices.add(dto.getHourIndex());
            } else {
                association.setHourIndex(nextAvailableIndex);
                usedIndices.add(nextAvailableIndex);
                nextAvailableIndex++;
            }
        } else {
            association.setIsHourEnable(false);
            association.setHourIndex(null);
        }

        if (Boolean.TRUE.equals(dto.getIsUserEnable())) {
            association.setIsUserEnable(true);
            if (dto.getUserIndex() != null) {
                if (usedIndices.contains(dto.getUserIndex())) {
                    throw new InvalidInputException("Índice duplicado: " + dto.getUserIndex());
                }
                association.setUserIndex(dto.getUserIndex());
                usedIndices.add(dto.getUserIndex());
            } else {
                association.setUserIndex(nextAvailableIndex);
                usedIndices.add(nextAvailableIndex);
                nextAvailableIndex++;
            }
        } else {
            association.setIsUserEnable(false);
            association.setUserIndex(null);
        }

        association.setIsReadEnable(dto.getIsReadEnable());
    }

    private void configureBaseColumnsForUpdate(
            UpdateTabulatePatternRequestDTO.InstrumentAssociationDTO dto,
            InstrumentTabulateAssociationEntity association,
            Set<Integer> usedIndices,
            int nextAvailableIndex) {

        if (Boolean.TRUE.equals(dto.getIsDateEnable())) {
            association.setIsDateEnable(true);
            if (dto.getDateIndex() != null) {
                if (usedIndices.contains(dto.getDateIndex())) {
                    throw new InvalidInputException("Índice duplicado: " + dto.getDateIndex());
                }
                association.setDateIndex(dto.getDateIndex());
                usedIndices.add(dto.getDateIndex());
            } else {
                association.setDateIndex(nextAvailableIndex);
                usedIndices.add(nextAvailableIndex);
                nextAvailableIndex++;
            }
        } else {
            association.setIsDateEnable(false);
            association.setDateIndex(null);
        }

        if (Boolean.TRUE.equals(dto.getIsHourEnable())) {
            association.setIsHourEnable(true);
            if (dto.getHourIndex() != null) {
                if (usedIndices.contains(dto.getHourIndex())) {
                    throw new InvalidInputException("Índice duplicado: " + dto.getHourIndex());
                }
                association.setHourIndex(dto.getHourIndex());
                usedIndices.add(dto.getHourIndex());
            } else {
                association.setHourIndex(nextAvailableIndex);
                usedIndices.add(nextAvailableIndex);
                nextAvailableIndex++;
            }
        } else {
            association.setIsHourEnable(false);
            association.setHourIndex(null);
        }

        if (Boolean.TRUE.equals(dto.getIsUserEnable())) {
            association.setIsUserEnable(true);
            if (dto.getUserIndex() != null) {
                if (usedIndices.contains(dto.getUserIndex())) {
                    throw new InvalidInputException("Índice duplicado: " + dto.getUserIndex());
                }
                association.setUserIndex(dto.getUserIndex());
                usedIndices.add(dto.getUserIndex());
            } else {
                association.setUserIndex(nextAvailableIndex);
                usedIndices.add(nextAvailableIndex);
                nextAvailableIndex++;
            }
        } else {
            association.setIsUserEnable(false);
            association.setUserIndex(null);
        }

        association.setIsReadEnable(dto.getIsReadEnable());
    }

    private void processOutputAssociations(
            List<CreateTabulatePatternRequestDTO.OutputAssociationDTO> outputDTOs,
            InstrumentTabulateAssociationEntity association,
            InstrumentEntity instrument,
            Set<Integer> usedIndices,
            int nextAvailableIndex) {

        if (outputDTOs.isEmpty()) {
            throw new InvalidInputException("Pelo menos uma associação de output é obrigatória para o instrumento "
                    + instrument.getName());
        }

        Set<Long> processedOutputIds = new HashSet<>();

        Set<InstrumentTabulateOutputAssociationEntity> outputAssociations = new HashSet<>();

        for (CreateTabulatePatternRequestDTO.OutputAssociationDTO dto : outputDTOs) {

            OutputEntity output = outputService.findById(dto.getOutputId());
            if (!output.getInstrument().getId().equals(instrument.getId())) {
                throw new InvalidInputException(
                        "O output com ID " + dto.getOutputId() + " não pertence ao instrumento com ID " + instrument.getId());
            }

            if (processedOutputIds.contains(dto.getOutputId())) {
                throw new InvalidInputException("Output duplicado: " + output.getName());
            }
            processedOutputIds.add(dto.getOutputId());

            int outputIndex = Optional.ofNullable(dto.getOutputIndex()).orElse(nextAvailableIndex);
            if (usedIndices.contains(outputIndex)) {
                throw new InvalidInputException("Índice duplicado: " + outputIndex);
            }
            usedIndices.add(outputIndex);

            InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
            outputAssociation.setAssociation(association);
            outputAssociation.setOutput(output);
            outputAssociation.setOutputIndex(outputIndex);

            outputAssociations.add(outputAssociation);

            nextAvailableIndex = getNextAvailableIndex(usedIndices);
        }

        association.setOutputAssociations(outputAssociations);
    }

    private void processOutputAssociationsForUpdate(
            List<UpdateTabulatePatternRequestDTO.OutputAssociationDTO> outputDTOs,
            InstrumentTabulateAssociationEntity association,
            InstrumentEntity instrument,
            Set<Integer> usedIndices,
            int nextAvailableIndex) {

        if (outputDTOs.isEmpty()) {
            throw new InvalidInputException("Pelo menos uma associação de output é obrigatória para o instrumento "
                    + instrument.getName());
        }

        Set<Long> processedOutputIds = new HashSet<>();

        Set<InstrumentTabulateOutputAssociationEntity> outputAssociations = new HashSet<>();

        for (UpdateTabulatePatternRequestDTO.OutputAssociationDTO dto : outputDTOs) {

            OutputEntity output = outputService.findById(dto.getOutputId());
            if (!output.getInstrument().getId().equals(instrument.getId())) {
                throw new InvalidInputException(
                        "O output com ID " + dto.getOutputId() + " não pertence ao instrumento com ID " + instrument.getId());
            }

            if (processedOutputIds.contains(dto.getOutputId())) {
                throw new InvalidInputException("Output duplicado: " + output.getName());
            }
            processedOutputIds.add(dto.getOutputId());

            int outputIndex = Optional.ofNullable(dto.getOutputIndex()).orElse(nextAvailableIndex);
            if (usedIndices.contains(outputIndex)) {
                throw new InvalidInputException("Índice duplicado: " + outputIndex);
            }
            usedIndices.add(outputIndex);

            InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
            outputAssociation.setId(dto.getId());
            outputAssociation.setAssociation(association);
            outputAssociation.setOutput(output);
            outputAssociation.setOutputIndex(outputIndex);

            outputAssociations.add(outputAssociation);

            nextAvailableIndex = getNextAvailableIndex(usedIndices);
        }

        association.setOutputAssociations(outputAssociations);
    }

    private int getNextAvailableIndex(Set<Integer> usedIndices) {
        int index = 1;
        while (usedIndices.contains(index)) {
            index++;
        }
        return index;
    }
}
