package com.geosegbar.infra.checklist_response.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.geosegbar.common.utils.ChecklistOptionTransitionValidator;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.AnswerPhotoEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.answer_photo.persistence.jpa.AnswerPhotoRepository;
import com.geosegbar.infra.checklist_response.dtos.AnswerUpdateDTO;
import com.geosegbar.infra.checklist_response.dtos.ChecklistResponseDetailDTO;
import com.geosegbar.infra.checklist_response.dtos.ChecklistResponseUpdateDTO;
import com.geosegbar.infra.checklist_response.dtos.ClientDetailedChecklistResponsesDTO;
import com.geosegbar.infra.checklist_response.dtos.DamInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.DamLastChecklistDTO;
import com.geosegbar.infra.checklist_response.dtos.OptionInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.PagedChecklistResponseDTO;
import com.geosegbar.infra.checklist_response.dtos.PhotoInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.QuestionWithAnswerDTO;
import com.geosegbar.infra.checklist_response.dtos.TemplateWithAnswersDTO;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChecklistResponseService {

    private final ChecklistResponseRepository checklistResponseRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final AnswerRepository answerRepository;
    private final AnswerPhotoRepository answerPhotoRepository;
    private final OptionRepository optionRepository;
    private final FileStorageService fileStorageService;
    private final DamService damService;
    private final ClientRepository clientRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ChecklistResponseEntity> findAll() {
        return checklistResponseRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ChecklistResponseEntity findById(Long id) {
        return checklistResponseRepository.findByIdWithBasicInfo(id)
                .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para id: " + id));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ChecklistResponseEntity> findByDamId(Long damId) {
        damService.findById(damId);
        return checklistResponseRepository.findByDamId(damId);
    }

    @Transactional
    public ChecklistResponseEntity save(ChecklistResponseEntity checklistResponse) {
        Long damId = checklistResponse.getDam().getId();

        DamEntity dam = damService.findById(damId);
        checklistResponse.setDam(dam);

        ChecklistResponseEntity saved = checklistResponseRepository.save(checklistResponse);

        return findById(saved.getId());
    }

    @Transactional
    public ChecklistResponseEntity update(ChecklistResponseEntity checklistResponse) {
        ChecklistResponseEntity existing = checklistResponseRepository.findById(checklistResponse.getId())
                .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para atualização!"));

        if (checklistResponse.getDam() != null && checklistResponse.getDam().getId() != null) {
            DamEntity newDam = damService.findById(checklistResponse.getDam().getId());
            checklistResponse.setDam(newDam);
        } else {
            checklistResponse.setDam(existing.getDam());
        }

        if (checklistResponse.getUser() == null) {
            checklistResponse.setUser(existing.getUser());
        }

        ChecklistResponseEntity saved = checklistResponseRepository.save(checklistResponse);

        return findById(saved.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        if (!checklistResponseRepository.existsById(id)) {
            throw new NotFoundException("Resposta de Checklist não encontrada para exclusão!");
        }
        checklistResponseRepository.deleteById(id);
    }

    @Transactional
    public void updateChecklistResponse(Long checklistResponseId, ChecklistResponseUpdateDTO dto) {

        ChecklistResponseEntity checklistResponse = checklistResponseRepository.findByIdWithBasicInfo(checklistResponseId)
                .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para id: " + checklistResponseId));

        boolean hasTopLevelChanges = dto.getUpstreamLevel() != null
                || dto.getDownstreamLevel() != null
                || dto.getSpilledFlow() != null
                || dto.getTurbinedFlow() != null
                || dto.getAccumulatedRainfall() != null
                || dto.getWeatherCondition() != null;

        if (hasTopLevelChanges) {
            checklistResponseRepository.updateTopLevelFields(
                    checklistResponseId,
                    formatToTwoDecimals(dto.getUpstreamLevel()),
                    formatToTwoDecimals(dto.getDownstreamLevel()),
                    formatToTwoDecimals(dto.getSpilledFlow()),
                    formatToTwoDecimals(dto.getTurbinedFlow()),
                    formatToTwoDecimals(dto.getAccumulatedRainfall()),
                    dto.getWeatherCondition());
        }

        if (dto.getAnswers() != null && !dto.getAnswers().isEmpty()) {
            updateAnswers(checklistResponse, dto.getAnswers());
        }
    }

    private void updateAnswers(ChecklistResponseEntity checklistResponse, List<AnswerUpdateDTO> answerUpdates) {
        Long checklistResponseId = checklistResponse.getId();

        List<Long> answerIds = answerUpdates.stream()
                .map(AnswerUpdateDTO::getAnswerId)
                .collect(Collectors.toList());

        List<AnswerEntity> answers = answerRepository.findByIdsAndChecklistResponseId(answerIds, checklistResponseId);

        if (answers.size() != answerIds.size()) {
            Set<Long> foundIds = answers.stream().map(AnswerEntity::getId).collect(Collectors.toSet());
            List<Long> missingIds = answerIds.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toList());
            throw new InvalidInputException(
                    "Respostas não encontradas ou não pertencem a esta resposta de checklist: " + missingIds);
        }

        Map<Long, AnswerEntity> answerMap = answers.stream()
                .collect(Collectors.toMap(AnswerEntity::getId, a -> a));

        Set<Long> allOptionIds = answerUpdates.stream()
                .map(AnswerUpdateDTO::getSelectedOptionId)
                .collect(Collectors.toSet());

        List<OptionEntity> options = optionRepository.findAllById(new ArrayList<>(allOptionIds));
        Map<Long, OptionEntity> optionMap = options.stream()
                .collect(Collectors.toMap(OptionEntity::getId, o -> o));

        if (optionMap.size() != allOptionIds.size()) {
            List<Long> missingOptionIds = allOptionIds.stream()
                    .filter(id -> !optionMap.containsKey(id)).collect(Collectors.toList());
            throw new NotFoundException("Opções não encontradas: " + missingOptionIds);
        }

        List<Long> questionIds = answers.stream()
                .map(a -> a.getQuestion().getId())
                .collect(Collectors.toList());

        Map<Long, String> previousLabels = new HashMap<>();
        List<Object[]> prevResults = answerRepository.findPreviousOptionLabels(
                questionIds,
                checklistResponse.getDam().getId(),
                checklistResponse.getChecklistId(),
                checklistResponse.getCreatedAt());
        for (Object[] row : prevResults) {
            previousLabels.put((Long) row[0], (String) row[1]);
        }

        for (AnswerUpdateDTO updateDto : answerUpdates) {
            AnswerEntity answer = answerMap.get(updateDto.getAnswerId());
            OptionEntity newOption = optionMap.get(updateDto.getSelectedOptionId());
            String newLabel = newOption.getLabel();
            String questionText = answer.getQuestion().getQuestionText();

            String previousLabel = previousLabels.get(answer.getQuestion().getId());
            ChecklistOptionTransitionValidator.validateTransition(previousLabel, newLabel, questionText);

            // Determina valores efetivos pos-update para validacao de evidencia
            String effectiveComment = updateDto.getComment() != null ? updateDto.getComment() : answer.getComment();
            boolean effectiveHasPhotos;
            if (updateDto.getPhotos() != null) {
                effectiveHasPhotos = !updateDto.getPhotos().isEmpty();
            } else {
                effectiveHasPhotos = answer.getPhotos() != null && !answer.getPhotos().isEmpty();
            }

            ChecklistOptionTransitionValidator.validateEvidence(newLabel, effectiveComment, effectiveHasPhotos, questionText);

            // Aplica opcao selecionada
            answer.getSelectedOptions().clear();
            answer.getSelectedOptions().add(newOption);

            // Aplica comentario se enviado
            if (updateDto.getComment() != null) {
                answer.setComment(updateDto.getComment());
            }

            // Aplica fotos se enviadas: apaga antigas do S3 + DB e salva novas
            if (updateDto.getPhotos() != null) {
                replaceAnswerPhotos(answer, updateDto.getPhotos());
            }
        }
    }

    private void replaceAnswerPhotos(AnswerEntity answer, List<PhotoSubmissionDTO> newPhotos) {
        // Usa a coleção já carregada pelo EntityGraph para deletar do S3
        // (evita query extra via findByAnswerId)
        for (AnswerPhotoEntity existing : new ArrayList<>(answer.getPhotos())) {
            fileStorageService.deleteFile(existing.getImagePath());
        }
        // orphanRemoval=true cuida do DELETE no banco ao limpar a coleção
        answer.getPhotos().clear();

        // Salva novas fotos
        for (PhotoSubmissionDTO photoDto : newPhotos) {
            try {
                String base64Image = photoDto.getBase64Image();
                if (base64Image != null && base64Image.contains(",")) {
                    base64Image = base64Image.split(",")[1];
                }
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);

                String photoUrl = fileStorageService.storeFileFromBytes(
                        imageBytes,
                        photoDto.getFileName(),
                        photoDto.getContentType(),
                        "answer-photos");

                AnswerPhotoEntity photo = new AnswerPhotoEntity();
                photo.setAnswer(answer);
                photo.setImagePath(photoUrl);
                answerPhotoRepository.save(photo);
                answer.getPhotos().add(photo);
            } catch (Exception e) {
                throw new FileStorageException("Erro ao processar imagem: " + e.getMessage());
            }
        }
    }

    private Double formatToTwoDecimals(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ChecklistResponseDetailDTO> findChecklistResponsesByDamId(Long damId) {
        damService.findById(damId);

        List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository.findByDamIdWithFullDetails(damId);

        return checklistResponses.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findChecklistResponsesByClientIdPaged(
            Long clientId, Pageable pageable) {

        Page<ChecklistResponseEntity> page = checklistResponseRepository.findByClientIdOptimized(clientId, pageable);
        return convertPageToResponse(page);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ChecklistResponseDetailDTO findChecklistResponseById(Long checklistResponseId) {

        ChecklistResponseEntity checklistResponse = findById(checklistResponseId);

        return convertToDetailDto(checklistResponse);
    }

    /**
     * Converte Entity para DTO carregando os detalhes profundos em uma query
     * separada. Essa abordagem evita o Produto Cartesiano (MultipleBagFetch) e
     * N+1.
     */
    private ChecklistResponseDetailDTO convertToDetailDto(ChecklistResponseEntity checklistResponse) {
        ChecklistResponseDetailDTO dto = new ChecklistResponseDetailDTO();
        dto.setId(checklistResponse.getId());
        dto.setChecklistName(checklistResponse.getChecklistName());
        dto.setChecklistId(checklistResponse.getChecklistId());
        dto.setCreatedAt(checklistResponse.getCreatedAt());

        if (checklistResponse.getUser() != null) {
            dto.setUserId(checklistResponse.getUser().getId());
            dto.setUserName(checklistResponse.getUser().getName());
        }

        dto.setUpstreamLevel(checklistResponse.getUpstreamLevel());
        dto.setDownstreamLevel(checklistResponse.getDownstreamLevel());
        dto.setSpilledFlow(checklistResponse.getSpilledFlow());
        dto.setTurbinedFlow(checklistResponse.getTurbinedFlow());
        dto.setAccumulatedRainfall(checklistResponse.getAccumulatedRainfall());
        dto.setWeatherCondition(checklistResponse.getWeatherCondition());

        DamEntity dam = checklistResponse.getDam();
        if (dam != null) {
            DamInfoDTO damInfo = new DamInfoDTO();
            damInfo.setId(dam.getId());
            damInfo.setName(dam.getName());
            dto.setDam(damInfo);
        }

        List<QuestionnaireResponseEntity> questionnaireResponses
                = questionnaireResponseRepository.findByChecklistResponseIdOptimized(checklistResponse.getId());

        if (questionnaireResponses.isEmpty()) {
            dto.setTemplates(new ArrayList<>());
            return dto;
        }

        Map<Long, TemplateWithAnswersDTO> templateMap = new HashMap<>();

        for (QuestionnaireResponseEntity qResponse : questionnaireResponses) {
            TemplateQuestionnaireEntity template = qResponse.getTemplateQuestionnaire();
            Long templateId = template.getId();

            TemplateWithAnswersDTO templateDto = templateMap.computeIfAbsent(templateId,
                    id -> new TemplateWithAnswersDTO(
                            templateId,
                            template.getName(),
                            qResponse.getId(),
                            new ArrayList<>()
                    ));

            for (AnswerEntity answer : qResponse.getAnswers()) {
                QuestionEntity question = answer.getQuestion();

                List<OptionInfoDTO> allQuestionOptions = question.getOptions().stream()
                        .map(opt -> new OptionInfoDTO(opt.getId(), opt.getLabel()))
                        .collect(Collectors.toList());

                List<PhotoInfoDTO> photoDtos = answer.getPhotos().stream()
                        .map(photo -> new PhotoInfoDTO(photo.getId(), photo.getImagePath()))
                        .collect(Collectors.toList());

                List<OptionInfoDTO> optionDtos = answer.getSelectedOptions().stream()
                        .map(option -> new OptionInfoDTO(option.getId(), option.getLabel()))
                        .collect(Collectors.toList());

                QuestionWithAnswerDTO questionWithAnswer = new QuestionWithAnswerDTO(
                        question.getId(),
                        question.getQuestionText(),
                        question.getType(),
                        answer.getId(),
                        answer.getComment(),
                        answer.getLatitude(),
                        answer.getLongitude(),
                        optionDtos,
                        photoDtos,
                        allQuestionOptions
                );

                templateDto.getQuestionsWithAnswers().add(questionWithAnswer);
            }
        }

        dto.setTemplates(new ArrayList<>(templateMap.values()));
        return dto;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ChecklistResponseDetailDTO> findChecklistResponsesByUserId(Long userId) {
        return checklistResponseRepository.findByUserId(userId).stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ChecklistResponseDetailDTO> findChecklistResponsesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return checklistResponseRepository.findByCreatedAtBetween(startDate, endDate).stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findChecklistResponsesByDamIdPaged(Long damId, Pageable pageable) {
        damService.findById(damId);
        Page<ChecklistResponseEntity> page = checklistResponseRepository.findByDamIdOptimized(damId, pageable);
        return convertPageToResponse(page);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findChecklistResponsesByUserIdPaged(Long userId, Pageable pageable) {
        Page<ChecklistResponseEntity> page = checklistResponseRepository.findByUserId(userId, pageable);
        return convertPageToResponse(page);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findChecklistResponsesByDateRangePaged(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<ChecklistResponseEntity> page = checklistResponseRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        return convertPageToResponse(page);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findAllChecklistResponsesPaged(Pageable pageable) {
        Page<ChecklistResponseEntity> page = checklistResponseRepository.findAll(pageable);
        return convertPageToResponse(page);
    }

    private PagedChecklistResponseDTO<ChecklistResponseDetailDTO> convertPageToResponse(Page<ChecklistResponseEntity> page) {
        List<ChecklistResponseDetailDTO> dtos = page.getContent().stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());

        return new PagedChecklistResponseDTO<>(
                dtos,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<DamLastChecklistDTO> getLastChecklistDateByClient(Long clientId) {
        List<DamEntity> dams = damService.findDamsByClientId(clientId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<DamLastChecklistDTO> result = new ArrayList<>();

        for (DamEntity dam : dams) {

            List<ChecklistResponseEntity> responses = checklistResponseRepository.findByDamId(dam.getId());
            if (responses.isEmpty()) {
                result.add(new DamLastChecklistDTO(dam.getId(), dam.getName(), "Nenhuma inspeção realizada."));
            } else {
                ChecklistResponseEntity last = responses.stream()
                        .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                        .orElse(null);
                String dateStr = last != null ? last.getCreatedAt().format(formatter) : "Nenhuma inspeção realizada.";
                result.add(new DamLastChecklistDTO(dam.getId(), dam.getName(), dateStr));
            }
        }
        return result;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ClientDetailedChecklistResponsesDTO findLatestDetailedChecklistResponsesByClientId(Long clientId, int limit) {

        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com ID: " + clientId));

        List<Long> responseIds = checklistResponseRepository.findLatestChecklistResponseIdsByClientIdAndLimit(clientId, limit);

        if (responseIds.isEmpty()) {
            return new ClientDetailedChecklistResponsesDTO(clientId, client.getName(), List.of());
        }

        Map<Long, ClientDetailedChecklistResponsesDTO.ChecklistWithDetailedResponsesDTO> checklistMap = new HashMap<>();

        for (Long id : responseIds) {

            checklistResponseRepository.findByIdWithBasicInfo(id).ifPresent(response -> {

                Long checklistId = response.getChecklistId();

                ClientDetailedChecklistResponsesDTO.ChecklistWithDetailedResponsesDTO checklistDto
                        = checklistMap.computeIfAbsent(
                                checklistId,
                                cid -> new ClientDetailedChecklistResponsesDTO.ChecklistWithDetailedResponsesDTO(
                                        checklistId,
                                        response.getChecklistName(),
                                        new ArrayList<>()
                                )
                        );

                ChecklistResponseDetailDTO detailedDto = convertToDetailDto(response);
                checklistDto.getLatestResponses().add(detailedDto);
            });
        }

        return new ClientDetailedChecklistResponsesDTO(
                clientId,
                client.getName(),
                new ArrayList<>(checklistMap.values())
        );
    }
}
