package com.geosegbar.infra.checklist_submission.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.AnomalyOriginEnum;
import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.entities.AnomalyPhotoEntity;
import com.geosegbar.entities.AnomalyStatusEntity;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.AnswerPhotoEntity;
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.anomaly_photo.persistence.jpa.AnomalyPhotoRepository;
import com.geosegbar.infra.anomaly_status.persistence.jpa.AnomalyStatusRepository;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.answer_photo.persistence.jpa.AnswerPhotoRepository;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.QuestionnaireResponseSubmissionDTO;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.danger_level.persistence.jpa.DangerLevelRepository;
import com.geosegbar.infra.documentation_dam.persistence.DocumentationDamRepository;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChecklistResponseSubmissionService {

    private final ChecklistResponseRepository checklistResponseRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final AnswerRepository answerRepository;
    private final AnswerPhotoRepository answerPhotoRepository;
    private final OptionRepository optionRepository;
    private final QuestionRepository questionRepository;
    private final TemplateQuestionnaireRepository templateQuestionnaireRepository;
    private final FileStorageService fileStorageService;
    private final DamService damService;
    private final UserRepository userRepository;
    private final ChecklistRepository checklistRepository;
    private final DangerLevelRepository dangerLevelRepository;
    private final AnomalyStatusRepository anomalyStatusRepository;
    private final AnomalyRepository anomalyRepository;
    private final PVAnswerValidator pvAnswerValidator;
    private final DamRepository damRepository;
    private final AnomalyPhotoRepository anomalyPhotoRepository;
    private final DocumentationDamRepository documentationDamRepository;

    @Transactional

    public ChecklistResponseEntity submitChecklistResponse(ChecklistResponseSubmissionDTO submissionDto) {
        validateUserAccessToDam(submissionDto.getUserId(), submissionDto.getDamId());

        if (!AuthenticatedUserUtil.isAdmin()) {
            if (submissionDto.isMobile()) {
                if (!AuthenticatedUserUtil.getCurrentUser().getRoutineInspectionPermission().getIsFillMobile()) {
                    throw new UnauthorizedException("Usuário não tem permissão para preencher checklist via mobile!");
                }
            } else if (!submissionDto.isMobile()) {
                if (!AuthenticatedUserUtil.getCurrentUser().getRoutineInspectionPermission().getIsFillWeb()) {
                    throw new UnauthorizedException("Usuário não tem permissão para preencher checklist via web!");
                }
            }
        }

        Map<Long, String> optionsCache = optionRepository.findAll().stream()
                .collect(Collectors.toMap(OptionEntity::getId, OptionEntity::getLabel));

        ChecklistResponseEntity checklistResponse = createChecklistResponse(submissionDto);

        validateAllRequiredQuestionnaires(submissionDto);

        validatePVAnswersHaveRequiredFields(submissionDto, optionsCache);

        for (QuestionnaireResponseSubmissionDTO questionnaireDto : submissionDto.getQuestionnaireResponses()) {
            validateAllQuestionsAnswered(questionnaireDto);

            QuestionnaireResponseEntity questionnaireResponse = createQuestionnaireResponse(questionnaireDto, checklistResponse);

            for (AnswerSubmissionDTO answerDto : questionnaireDto.getAnswers()) {

                validateCriticalAnswerRequirements(answerDto, optionsCache);

                createAnswer(answerDto, questionnaireResponse, optionsCache);

                if (pvAnswerValidator.isPVAnswer(answerDto, optionsCache)) {
                    createAnomalyFromPVAnswer(
                            answerDto,
                            submissionDto.getUserId(),
                            submissionDto.getDamId(),
                            questionnaireDto.getTemplateQuestionnaireId()
                    );
                }
            }
        }

        updateLastAchievementChecklist(submissionDto.getDamId());

        return checklistResponse;
    }

    private void validateCriticalAnswerRequirements(AnswerSubmissionDTO answerDto, Map<Long, String> optionsCache) {
        if (answerDto.getSelectedOptionIds() == null || answerDto.getSelectedOptionIds().isEmpty()) {
            return;
        }

        boolean isCritical = false;
        List<String> foundLabels = new ArrayList<>();

        Set<String> criticalLabels = Set.of("AU", "DM", "PC", "DS");

        for (Long id : answerDto.getSelectedOptionIds()) {
            String label = optionsCache.get(id);
            if (label != null && criticalLabels.contains(label)) {
                isCritical = true;
                foundLabels.add(label);
            }
        }

        if (isCritical) {
            List<String> missingFields = new ArrayList<>();

            if (answerDto.getComment() == null || answerDto.getComment().trim().isEmpty()) {
                missingFields.add("Comentário/Observação");
            }

            if (answerDto.getPhotos() == null || answerDto.getPhotos().isEmpty()) {
                missingFields.add("Foto");
            }

            if (!missingFields.isEmpty()) {

                String questionText = questionRepository.findById(answerDto.getQuestionId())
                        .map(QuestionEntity::getQuestionText)
                        .orElse("Desconhecida");

                throw new InvalidInputException(String.format(
                        "A resposta para a pergunta '%s' foi marcada com %s e exige obrigatoriamente: %s.",
                        questionText,
                        foundLabels,
                        String.join(" e ", missingFields)
                ));
            }
        }
    }

    private void validatePVAnswersHaveRequiredFields(ChecklistResponseSubmissionDTO submissionDto, Map<Long, String> optionsCache) {
        for (QuestionnaireResponseSubmissionDTO questionnaireDto : submissionDto.getQuestionnaireResponses()) {
            for (AnswerSubmissionDTO answerDto : questionnaireDto.getAnswers()) {
                if (pvAnswerValidator.isPVAnswer(answerDto, optionsCache)) {
                    QuestionEntity question = questionRepository.findById(answerDto.getQuestionId())
                            .orElseThrow(() -> new NotFoundException("Pergunta não encontrada: " + answerDto.getQuestionId()));

                    pvAnswerValidator.validatePVAnswer(answerDto, question.getQuestionText(), optionsCache);
                }
            }
        }
    }

    private void updateLastAchievementChecklist(Long damId) {
        documentationDamRepository.findByDamId(damId).ifPresent(documentationDam -> {
            documentationDam.setLastAchievementChecklist(LocalDate.now());
            documentationDamRepository.save(documentationDam);
        });
    }

    private void validateUserAccessToDam(Long userId, Long damId) {
        UserEntity user = userRepository.findByIdWithClients(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        DamEntity dam = damRepository.findById(damId)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));

        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }

        boolean userBelongsToClient = user.getClients().stream()
                .anyMatch(client -> client.getId().equals(dam.getClient().getId()));

        if (!userBelongsToClient) {
            throw new UnauthorizedException(
                    "Usuário não tem permissão para acessar esta barragem. "
                    + "O usuário não pertence ao cliente proprietário da barragem."
            );
        }

        boolean hasSpecificPermission = user.getDamPermissions().stream()
                .anyMatch(permission
                        -> permission.getDam().getId().equals(damId)
                && permission.getHasAccess()
                && permission.getClient().getId().equals(dam.getClient().getId())
                );

        if (!hasSpecificPermission) {
            throw new UnauthorizedException(
                    "Usuário não tem permissão específica para acessar esta barragem. "
                    + "Verifique as permissões de acesso na administração do sistema."
            );
        }
    }

    private void createAnomalyFromPVAnswer(
            AnswerSubmissionDTO answerDto,
            Long userId,
            Long damId,
            Long questionnaireId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        DamEntity dam = damRepository.findById(damId)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));

        DangerLevelEntity dangerLevel = dangerLevelRepository.findById(answerDto.getAnomalyDangerLevelId())
                .orElseThrow(() -> new NotFoundException("Nível de perigo não encontrado!"));

        AnomalyStatusEntity status = anomalyStatusRepository.findById(answerDto.getAnomalyStatusId())
                .orElseThrow(() -> new NotFoundException("Status não encontrado!"));

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setUser(user);
        anomaly.setDam(dam);
        anomaly.setLatitude(answerDto.getLatitude());
        anomaly.setLongitude(answerDto.getLongitude());
        anomaly.setQuestionnaireId(questionnaireId);
        anomaly.setQuestionId(answerDto.getQuestionId());
        anomaly.setOrigin(AnomalyOriginEnum.CHECKLIST);
        anomaly.setObservation(answerDto.getComment());
        anomaly.setRecommendation(answerDto.getAnomalyRecommendation());
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);

        AnomalyEntity savedAnomaly = anomalyRepository.save(anomaly);

        if (answerDto.getPhotos() != null && !answerDto.getPhotos().isEmpty()) {
            for (PhotoSubmissionDTO photoDto : answerDto.getPhotos()) {
                saveAnomalyPhoto(photoDto, savedAnomaly, dam.getId());
            }
        }
    }

    private void saveAnomalyPhoto(PhotoSubmissionDTO photoDto, AnomalyEntity anomaly, Long damId) {
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
            anomalyPhotoRepository.save(photoEntity);
        } catch (Exception e) {
            throw new FileStorageException("Erro ao processar imagem da anomalia: " + e.getMessage());
        }
    }

    private void validateAllRequiredQuestionnaires(ChecklistResponseSubmissionDTO submissionDto) {
        ChecklistEntity checklist = checklistRepository.findById(submissionDto.getChecklistId())
                .orElseThrow(() -> new NotFoundException("Checklist não encontrado com o ID: " + submissionDto.getChecklistId()));

        Set<Long> requiredTemplateIds = checklist.getTemplateQuestionnaires().stream()
                .map(TemplateQuestionnaireEntity::getId)
                .collect(Collectors.toSet());

        List<Long> submittedTemplateIdsList = submissionDto.getQuestionnaireResponses().stream()
                .map(QuestionnaireResponseSubmissionDTO::getTemplateQuestionnaireId)
                .collect(Collectors.toList());

        Set<Long> submittedTemplateIdsSet = new HashSet<>(submittedTemplateIdsList);

        if (submittedTemplateIdsSet.size() < submittedTemplateIdsList.size()) {
            throw new InvalidInputException("Existem questionários duplicados na submissão do checklist '" + checklist.getName() + "'.");
        }

        if (!submittedTemplateIdsSet.containsAll(requiredTemplateIds)) {
            Set<Long> missingTemplateIds = new HashSet<>(requiredTemplateIds);
            missingTemplateIds.removeAll(submittedTemplateIdsSet);

            List<String> missingNames = templateQuestionnaireRepository.findAllById(missingTemplateIds).stream()
                    .map(TemplateQuestionnaireEntity::getName)
                    .collect(Collectors.toList());

            throw new InvalidInputException(String.format(
                    "Checklist incompleto. Faltam os questionários: %s",
                    String.join(", ", missingNames)
            ));
        }

        if (!requiredTemplateIds.containsAll(submittedTemplateIdsSet)) {
            Set<Long> extraTemplateIds = new HashSet<>(submittedTemplateIdsSet);
            extraTemplateIds.removeAll(requiredTemplateIds);

            throw new InvalidInputException(String.format(
                    "Checklist inválido. Foram enviados questionários que não pertencem a este checklist (IDs: %s).",
                    extraTemplateIds
            ));
        }
    }

    private void validateAllQuestionsAnswered(QuestionnaireResponseSubmissionDTO questionnaireDto) {
        TemplateQuestionnaireEntity template = templateQuestionnaireRepository
                .findById(questionnaireDto.getTemplateQuestionnaireId())
                .orElseThrow(() -> new NotFoundException("Modelo de questionário não encontrado: "
                + questionnaireDto.getTemplateQuestionnaireId()));

        Set<Long> expectedQuestionIds = template.getTemplateQuestions().stream()
                .map(tq -> tq.getQuestion().getId())
                .collect(Collectors.toSet());

        List<Long> submittedQuestionIdsList = questionnaireDto.getAnswers().stream()
                .map(AnswerSubmissionDTO::getQuestionId)
                .collect(Collectors.toList());

        Set<Long> uniqueSubmittedIds = new HashSet<>(submittedQuestionIdsList);

        if (uniqueSubmittedIds.size() < submittedQuestionIdsList.size()) {
            throw new InvalidInputException("Existem respostas duplicadas para a mesma pergunta no questionário '" + template.getName() + "'. Verifique os dados enviados.");
        }

        if (!uniqueSubmittedIds.containsAll(expectedQuestionIds)) {
            Set<Long> missingIds = new HashSet<>(expectedQuestionIds);
            missingIds.removeAll(uniqueSubmittedIds);

            List<String> missingTexts = questionRepository.findAllById(missingIds).stream()
                    .map(QuestionEntity::getQuestionText)
                    .collect(Collectors.toList());

            throw new InvalidInputException(String.format(
                    "O checklist mudou ou está incompleto. As seguintes perguntas não foram respondidas no questionário '%s': %s",
                    template.getName(),
                    String.join(", ", missingTexts)
            ));
        }

        if (!expectedQuestionIds.containsAll(uniqueSubmittedIds)) {
            Set<Long> extraIds = new HashSet<>(uniqueSubmittedIds);
            extraIds.removeAll(expectedQuestionIds);

            throw new InvalidInputException(String.format(
                    "O checklist mudou. Sua submissão contém perguntas que não pertencem mais ao questionário '%s' (IDs extras: %s). Atualize seu aplicativo.",
                    template.getName(),
                    extraIds
            ));
        }
    }

    private ChecklistResponseEntity createChecklistResponse(ChecklistResponseSubmissionDTO submissionDto) {
        DamEntity dam = damService.findById(submissionDto.getDamId());
        UserEntity user = userRepository.findById(submissionDto.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        ChecklistResponseEntity checklistResponse = new ChecklistResponseEntity();
        checklistResponse.setChecklistName(submissionDto.getChecklistName());
        checklistResponse.setChecklistId(submissionDto.getChecklistId());
        checklistResponse.setDam(dam);
        checklistResponse.setUser(user);
        checklistResponse.setUpstreamLevel(formatToTwoDecimals(submissionDto.getUpstreamLevel()));
        checklistResponse.setDownstreamLevel(formatToTwoDecimals(submissionDto.getDownstreamLevel()));
        checklistResponse.setSpilledFlow(formatToTwoDecimals(submissionDto.getSpilledFlow()));
        checklistResponse.setTurbinedFlow(formatToTwoDecimals(submissionDto.getTurbinedFlow()));
        checklistResponse.setAccumulatedRainfall(formatToTwoDecimals(submissionDto.getAccumulatedRainfall()));
        checklistResponse.setWeatherCondition(submissionDto.getWeatherCondition());

        return checklistResponseRepository.save(checklistResponse);
    }

    private Double formatToTwoDecimals(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private QuestionnaireResponseEntity createQuestionnaireResponse(
            QuestionnaireResponseSubmissionDTO questionnaireDto,
            ChecklistResponseEntity checklistResponse) {

        TemplateQuestionnaireEntity templateQuestionnaire = templateQuestionnaireRepository
                .findById(questionnaireDto.getTemplateQuestionnaireId())
                .orElseThrow(() -> new NotFoundException("Modelo de questionário não encontrado!"));

        QuestionnaireResponseEntity questionnaireResponse = new QuestionnaireResponseEntity();
        questionnaireResponse.setTemplateQuestionnaire(templateQuestionnaire);
        questionnaireResponse.setChecklistResponse(checklistResponse);
        questionnaireResponse.setDam(checklistResponse.getDam());

        return questionnaireResponseRepository.save(questionnaireResponse);
    }

    private AnswerEntity createAnswer(AnswerSubmissionDTO answerDto, QuestionnaireResponseEntity questionnaireResponse, Map<Long, String> optionsCache) {
        QuestionEntity question = questionRepository
                .findById(answerDto.getQuestionId())
                .orElseThrow(() -> new NotFoundException("Pergunta não encontrada: " + answerDto.getQuestionId()));

        AnswerEntity answer = new AnswerEntity();
        answer.setQuestion(question);
        answer.setQuestionnaireResponse(questionnaireResponse);
        answer.setLatitude(answerDto.getLatitude());
        answer.setLongitude(answerDto.getLongitude());

        if (TypeQuestionEnum.TEXT.equals(question.getType())) {
            if (answerDto.getComment() == null || answerDto.getComment().trim().isEmpty()) {
                throw new InvalidInputException("A pergunta '" + question.getQuestionText()
                        + "' é do tipo TEXT e requer uma resposta textual!");
            }
            answer.setComment(answerDto.getComment());
        } else if (TypeQuestionEnum.CHECKBOX.equals(question.getType())) {
            if (answerDto.getSelectedOptionIds() == null || answerDto.getSelectedOptionIds().isEmpty()) {
                throw new InvalidInputException("A pergunta '" + question.getQuestionText()
                        + "' é do tipo CHECKBOX e requer ao menos uma opção selecionada!");
            }

            Set<OptionEntity> options = new HashSet<>();
            for (Long optionId : answerDto.getSelectedOptionIds()) {
                if (!optionsCache.containsKey(optionId)) {
                    throw new NotFoundException("Opção inválida ou não encontrada: " + optionId);
                }

                options.add(optionRepository.getReferenceById(optionId));
            }
            answer.setSelectedOptions(options);
            answer.setComment(answerDto.getComment());
        }

        AnswerEntity savedAnswer = answerRepository.save(answer);

        if (answerDto.getPhotos() != null && !answerDto.getPhotos().isEmpty()) {
            for (PhotoSubmissionDTO photoDto : answerDto.getPhotos()) {
                saveAnswerPhoto(photoDto, savedAnswer);
            }
        }

        return savedAnswer;
    }

    private AnswerPhotoEntity saveAnswerPhoto(PhotoSubmissionDTO photoDto, AnswerEntity answer) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(photoDto.getBase64Image());

            String photoUrl = fileStorageService.storeFileFromBytes(
                    imageBytes,
                    photoDto.getFileName(),
                    photoDto.getContentType(),
                    "answer-photos"
            );

            AnswerPhotoEntity photo = new AnswerPhotoEntity();
            photo.setAnswer(answer);
            photo.setImagePath(photoUrl);

            return answerPhotoRepository.save(photo);

        } catch (Exception e) {
            throw new FileStorageException("Erro ao processar imagem: " + e.getMessage());
        }
    }
}
