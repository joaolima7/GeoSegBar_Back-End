package com.geosegbar.infra.instrument_tabulate_pattern.services;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    @CacheEvict(value = {"tabulatePatterns", "tabulatePatternsByDam"}, allEntries = true)
    public TabulatePatternResponseDTO create(CreateTabulatePatternRequestDTO request) {
        // Verificar se já existe um padrão com o mesmo nome na barragem
        if (patternRepository.existsByNameAndDamId(request.getName(), request.getDamId())) {
            throw new DuplicateResourceException(
                    "Já existe um padrão de tabela com o nome '" + request.getName() + "' nesta barragem!");
        }

        // Verificar se a barragem existe
        DamEntity dam = damService.findById(request.getDamId());

        // Verificar se a pasta existe (se fornecida)
        InstrumentTabulatePatternFolder folder = null;
        if (request.getFolderId() != null) {
            folder = folderService.findById(request.getFolderId());

            // Verificar se a pasta pertence à mesma barragem
            if (!folder.getDam().getId().equals(dam.getId())) {
                throw new InvalidInputException(
                        "A pasta selecionada não pertence à barragem informada!");
            }
        }

        // Criar o padrão
        InstrumentTabulatePatternEntity pattern = new InstrumentTabulatePatternEntity();
        pattern.setName(request.getName());
        pattern.setDam(dam);
        pattern.setFolder(folder);
        pattern.setIsLinimetricRulerEnable(request.getIsLinimetricRulerEnable());

        // Salvar primeiro para obter o ID
        pattern = patternRepository.save(pattern);

        // Adicionar associações de instrumentos
        Set<InstrumentTabulateAssociationEntity> associations = new HashSet<>();
        validateAndProcessInstrumentAssociations(request.getAssociations(), pattern, associations);

        pattern.setAssociations(associations);

        // Salvar novamente com as associações
        pattern = patternRepository.save(pattern);

        log.info("Padrão de tabela criado: id={}, name={}, damId={}, folderId={}",
                pattern.getId(), pattern.getName(), dam.getId(),
                folder != null ? folder.getId() : "sem pasta");

        return mapper.mapToResponseDTO(patternRepository.findByIdWithAllDetails(pattern.getId())
                .orElseThrow(() -> new NotFoundException("Padrão de tabela não encontrado após criação")));
    }

    @Transactional
    @CacheEvict(value = {"tabulatePatterns", "tabulatePatternsByDam"}, allEntries = true)
    public void delete(Long patternId) {
        InstrumentTabulatePatternEntity pattern = patternRepository.findByIdWithBasicDetails(patternId)
                .orElseThrow(() -> new NotFoundException("Padrão de tabela não encontrado com ID: " + patternId));

        log.info("Excluindo padrão de tabela: id={}, name={}, damId={}, folderId={}",
                pattern.getId(), pattern.getName(),
                pattern.getDam() != null ? pattern.getDam().getId() : null,
                pattern.getFolder() != null ? pattern.getFolder().getId() : null);

        // Cascade remove automaticamente as associações e outputs por causa do orphanRemoval = true
        patternRepository.delete(pattern);

        log.info("Padrão de tabela excluído com sucesso: id={}", patternId);
    }

    @Transactional
    @CacheEvict(value = {"tabulatePatterns", "tabulatePatternsByDam"}, allEntries = true)
    public TabulatePatternResponseDTO update(Long patternId, UpdateTabulatePatternRequestDTO request) {
        // Buscar o padrão com todos os detalhes
        InstrumentTabulatePatternEntity pattern = findEntityByIdWithAllDetails(patternId);

        // Verificar se o nome já existe para outro padrão na mesma barragem
        if (!pattern.getName().equals(request.getName())
                && patternRepository.existsByNameAndDamIdAndIdNot(request.getName(), pattern.getDam().getId(), patternId)) {
            throw new DuplicateResourceException(
                    "Já existe um padrão de tabela com o nome '" + request.getName() + "' nesta barragem!");
        }

        // Verificar se a pasta existe (se fornecida)
        InstrumentTabulatePatternFolder folder = null;
        if (request.getFolderId() != null) {
            folder = folderService.findById(request.getFolderId());

            // Verificar se a pasta pertence à mesma barragem
            if (!folder.getDam().getId().equals(pattern.getDam().getId())) {
                throw new InvalidInputException(
                        "A pasta selecionada não pertence à barragem do padrão!");
            }
        }

        // Atualizar campos básicos
        pattern.setName(request.getName());
        pattern.setFolder(folder); // Pode ser null para remover da pasta
        pattern.setIsLinimetricRulerEnable(request.getIsLinimetricRulerEnable());

        // Atualizar associações de instrumentos
        Set<InstrumentTabulateAssociationEntity> currentAssociations = pattern.getAssociations();
        Set<InstrumentTabulateAssociationEntity> newAssociations = new HashSet<>();

        // Mapear IDs das associações atuais para fácil acesso
        Map<Long, InstrumentTabulateAssociationEntity> associationsMap = currentAssociations.stream()
                .collect(Collectors.toMap(InstrumentTabulateAssociationEntity::getId, a -> a));

        // Processar associações atualizadas
        validateAndUpdateInstrumentAssociations(request.getAssociations(), pattern, newAssociations, associationsMap);

        // Substituir todas as associações
        pattern.getAssociations().clear();
        pattern.getAssociations().addAll(newAssociations);

        // Salvar as alterações
        pattern = patternRepository.save(pattern);

        log.info("Padrão de tabela atualizado: id={}, name={}, damId={}, folderId={}",
                pattern.getId(), pattern.getName(), pattern.getDam().getId(),
                folder != null ? folder.getId() : "sem pasta");

        return mapper.mapToResponseDTO(patternRepository.findByIdWithAllDetails(pattern.getId())
                .orElseThrow(() -> new NotFoundException("Padrão de tabela não encontrado após atualização")));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tabulatePatterns", key = "#patternId")
    public TabulatePatternResponseDTO findById(Long patternId) {
        InstrumentTabulatePatternEntity pattern = findEntityByIdWithAllDetails(patternId);
        return mapper.mapToResponseDTO(pattern);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "tabulatePatternsByDam", key = "#damId")
    public List<TabulatePatternResponseDTO> findByDamId(Long damId) {
        // Verificar se a barragem existe
        damService.findById(damId);

        List<InstrumentTabulatePatternEntity> patterns = patternRepository.findByDamIdWithAllDetails(damId);
        return patterns.stream()
                .map(mapper::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TabulatePatternResponseDTO> findByFolderId(Long folderId) {
        // Verificar se a pasta existe
        folderService.findById(folderId);

        List<InstrumentTabulatePatternEntity> patterns = patternRepository.findByFolderIdWithAllDetails(folderId);
        return patterns.stream()
                .map(mapper::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // Método para buscar entidade com todos os detalhes carregados
    public InstrumentTabulatePatternEntity findEntityByIdWithAllDetails(Long patternId) {
        return patternRepository.findByIdWithAllDetails(patternId)
                .orElseThrow(() -> new NotFoundException("Padrão de tabela não encontrado com ID: " + patternId));
    }

    // Métodos auxiliares privados
    private void validateAndProcessInstrumentAssociations(
            List<CreateTabulatePatternRequestDTO.InstrumentAssociationDTO> associationDTOs,
            InstrumentTabulatePatternEntity pattern,
            Set<InstrumentTabulateAssociationEntity> associations) {

        // Mapa para verificar índices únicos
        Set<Integer> usedIndices = new HashSet<>();
        int nextAvailableIndex = 1; // Começar do índice 1

        for (CreateTabulatePatternRequestDTO.InstrumentAssociationDTO dto : associationDTOs) {
            // Verificar se o instrumento existe e pertence à mesma barragem
            InstrumentEntity instrument = instrumentService.findById(dto.getInstrumentId());
            if (!instrument.getDam().getId().equals(pattern.getDam().getId())) {
                throw new InvalidInputException(
                        "O instrumento com ID " + dto.getInstrumentId() + " não pertence à barragem do padrão!");
            }

            // Criar associação de instrumento
            InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
            association.setPattern(pattern);
            association.setInstrument(instrument);

            // Configurar colunas base
            configureBaseColumns(dto, association, usedIndices, nextAvailableIndex);
            nextAvailableIndex = getNextAvailableIndex(usedIndices);

            // Processar associações de output
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

        // Mapa para verificar índices únicos
        Set<Integer> usedIndices = new HashSet<>();
        int nextAvailableIndex = 1; // Começar do índice 1

        for (UpdateTabulatePatternRequestDTO.InstrumentAssociationDTO dto : associationDTOs) {
            InstrumentTabulateAssociationEntity association;

            // Verificar se é atualização ou nova associação
            if (dto.getId() != null && existingAssociationsMap.containsKey(dto.getId())) {
                // Atualizar associação existente
                association = existingAssociationsMap.get(dto.getId());

                // Verificar se o instrumento foi alterado
                if (!association.getInstrument().getId().equals(dto.getInstrumentId())) {
                    // Instrumento alterado, verificar o novo
                    InstrumentEntity newInstrument = instrumentService.findById(dto.getInstrumentId());
                    if (!newInstrument.getDam().getId().equals(pattern.getDam().getId())) {
                        throw new InvalidInputException(
                                "O instrumento com ID " + dto.getInstrumentId() + " não pertence à barragem do padrão!");
                    }
                    association.setInstrument(newInstrument);
                }

                // Limpar associações de output existentes
                association.getOutputAssociations().clear();
            } else {
                // Criar nova associação
                association = new InstrumentTabulateAssociationEntity();
                association.setPattern(pattern);

                // Verificar se o instrumento existe e pertence à mesma barragem
                InstrumentEntity instrument = instrumentService.findById(dto.getInstrumentId());
                if (!instrument.getDam().getId().equals(pattern.getDam().getId())) {
                    throw new InvalidInputException(
                            "O instrumento com ID " + dto.getInstrumentId() + " não pertence à barragem do padrão!");
                }
                association.setInstrument(instrument);
            }

            // Configurar colunas base
            configureBaseColumnsForUpdate(dto, association, usedIndices, nextAvailableIndex);
            nextAvailableIndex = getNextAvailableIndex(usedIndices);

            // Processar associações de output
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

        // Configurar data
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

        // Configurar hora
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

        // Configurar usuário
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

        // Configurar leitura
        association.setIsReadEnable(dto.getIsReadEnable());
    }

    private void configureBaseColumnsForUpdate(
            UpdateTabulatePatternRequestDTO.InstrumentAssociationDTO dto,
            InstrumentTabulateAssociationEntity association,
            Set<Integer> usedIndices,
            int nextAvailableIndex) {

        // Mesmo padrão que configureBaseColumns, mas para DTOs de atualização
        // Configurar data
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

        // Configurar hora
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

        // Configurar usuário
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

        // Configurar leitura
        association.setIsReadEnable(dto.getIsReadEnable());
    }

    private void processOutputAssociations(
            List<CreateTabulatePatternRequestDTO.OutputAssociationDTO> outputDTOs,
            InstrumentTabulateAssociationEntity association,
            InstrumentEntity instrument,
            Set<Integer> usedIndices,
            int nextAvailableIndex) {

        // Validar se há pelo menos uma associação de output
        if (outputDTOs.isEmpty()) {
            throw new InvalidInputException("Pelo menos uma associação de output é obrigatória para o instrumento "
                    + instrument.getName());
        }

        // Criar um mapa para verificar outputs duplicados
        Set<Long> processedOutputIds = new HashSet<>();

        // Criar as associações de output
        Set<InstrumentTabulateOutputAssociationEntity> outputAssociations = new HashSet<>();

        for (CreateTabulatePatternRequestDTO.OutputAssociationDTO dto : outputDTOs) {
            // Verificar se o output existe e pertence ao instrumento
            OutputEntity output = outputService.findById(dto.getOutputId());
            if (!output.getInstrument().getId().equals(instrument.getId())) {
                throw new InvalidInputException(
                        "O output com ID " + dto.getOutputId() + " não pertence ao instrumento com ID " + instrument.getId());
            }

            // Verificar se o output já foi processado
            if (processedOutputIds.contains(dto.getOutputId())) {
                throw new InvalidInputException("Output duplicado: " + output.getName());
            }
            processedOutputIds.add(dto.getOutputId());

            // Verificar índice do output
            int outputIndex = dto.getOutputIndex() != null ? dto.getOutputIndex() : nextAvailableIndex;
            if (usedIndices.contains(outputIndex)) {
                throw new InvalidInputException("Índice duplicado: " + outputIndex);
            }
            usedIndices.add(outputIndex);

            // Criar associação de output
            InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
            outputAssociation.setAssociation(association);
            outputAssociation.setOutput(output);
            outputAssociation.setOutputIndex(outputIndex);

            outputAssociations.add(outputAssociation);

            // Incrementar o próximo índice disponível
            nextAvailableIndex = getNextAvailableIndex(usedIndices);
        }

        // Associar as outputs à associação do instrumento
        association.setOutputAssociations(outputAssociations);
    }

    private void processOutputAssociationsForUpdate(
            List<UpdateTabulatePatternRequestDTO.OutputAssociationDTO> outputDTOs,
            InstrumentTabulateAssociationEntity association,
            InstrumentEntity instrument,
            Set<Integer> usedIndices,
            int nextAvailableIndex) {

        // Validar se há pelo menos uma associação de output
        if (outputDTOs.isEmpty()) {
            throw new InvalidInputException("Pelo menos uma associação de output é obrigatória para o instrumento "
                    + instrument.getName());
        }

        // Criar um mapa para verificar outputs duplicados
        Set<Long> processedOutputIds = new HashSet<>();

        // Criar as associações de output
        Set<InstrumentTabulateOutputAssociationEntity> outputAssociations = new HashSet<>();

        for (UpdateTabulatePatternRequestDTO.OutputAssociationDTO dto : outputDTOs) {
            // Verificar se o output existe e pertence ao instrumento
            OutputEntity output = outputService.findById(dto.getOutputId());
            if (!output.getInstrument().getId().equals(instrument.getId())) {
                throw new InvalidInputException(
                        "O output com ID " + dto.getOutputId() + " não pertence ao instrumento com ID " + instrument.getId());
            }

            // Verificar se o output já foi processado
            if (processedOutputIds.contains(dto.getOutputId())) {
                throw new InvalidInputException("Output duplicado: " + output.getName());
            }
            processedOutputIds.add(dto.getOutputId());

            // Verificar índice do output
            int outputIndex = dto.getOutputIndex() != null ? dto.getOutputIndex() : nextAvailableIndex;
            if (usedIndices.contains(outputIndex)) {
                throw new InvalidInputException("Índice duplicado: " + outputIndex);
            }
            usedIndices.add(outputIndex);

            // Criar associação de output
            InstrumentTabulateOutputAssociationEntity outputAssociation = new InstrumentTabulateOutputAssociationEntity();
            outputAssociation.setId(dto.getId()); // Pode ser null para novas associações
            outputAssociation.setAssociation(association);
            outputAssociation.setOutput(output);
            outputAssociation.setOutputIndex(outputIndex);

            outputAssociations.add(outputAssociation);

            // Incrementar o próximo índice disponível
            nextAvailableIndex = getNextAvailableIndex(usedIndices);
        }

        // Associar as outputs à associação do instrumento
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
