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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.AnomalyOriginEnum;
import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.common.utils.ChecklistOptionTransitionValidator;
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
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.anomaly_photo.persistence.jpa.AnomalyPhotoRepository;
import com.geosegbar.infra.anomaly_status.persistence.jpa.AnomalyStatusRepository;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.answer_photo.persistence.jpa.AnswerPhotoRepository;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.OtherSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.QuestionnaireResponseSubmissionDTO;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.danger_level.persistence.jpa.DangerLevelRepository;
import com.geosegbar.infra.documentation_dam.persistence.DocumentationDamRepository;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChecklistSubmissionPersistenceService {

    private final ChecklistResponseRepository checklistResponseRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final AnswerRepository answerRepository;
    private final AnswerPhotoRepository answerPhotoRepository;
    private final OptionRepository optionRepository;
    private final QuestionRepository questionRepository;
    private final TemplateQuestionnaireRepository templateQuestionnaireRepository;
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

    @Transactional(timeout = 60)
    public ChecklistResponseSubmissionService.SubmissionResult persistChecklistData(
            ChecklistResponseSubmissionDTO submissionDto) {

        validateUserAccessToDam(submissionDto.getUserId(), submissionDto.getDamId());

        if (!AuthenticatedUserUtil.isAdmin()) {

            UserEntity currentUser = AuthenticatedUserUtil.getCurrentUser();

            if (currentUser.getRoutineInspectionPermission() == null) {
                currentUser = userRepository.findByIdWithPermissions(currentUser.getId())
                        .orElseThrow(() -> new NotFoundException("Usuário logado não encontrado"));
            }

            if (currentUser.getRoutineInspectionPermission() == null) {
                throw new ForbiddenException("Usuário não tem permissão para preencher checklist!");
            }

            if (submissionDto.isMobile()) {
                if (!Boolean.TRUE.equals(currentUser.getRoutineInspectionPermission().getIsFillMobile())) {
                    throw new ForbiddenException("Usuário não tem permissão para preencher checklist via mobile!");
                }
            } else {
                if (!Boolean.TRUE.equals(currentUser.getRoutineInspectionPermission().getIsFillWeb())) {
                    throw new ForbiddenException("Usuário não tem permissão para preencher checklist via web!");
                }
            }
        }

        Map<Long, String> optionsCache = loadOptionsCache(submissionDto);

        validateSubmission(submissionDto, optionsCache);

        ChecklistResponseEntity checklistResponse = createChecklistResponse(submissionDto);

        List<ChecklistResponseSubmissionService.PendingPhotoUpload> pendingUploads = new ArrayList<>();

        for (QuestionnaireResponseSubmissionDTO questionnaireDto : submissionDto.getQuestionnaireResponses()) {

            QuestionnaireResponseEntity questionnaireResponse = createQuestionnaireResponse(questionnaireDto, checklistResponse);

            for (AnswerSubmissionDTO answerDto : questionnaireDto.getAnswers()) {

                createAnswer(answerDto, questionnaireResponse, optionsCache, pendingUploads);

                if (pvAnswerValidator.isPVAnswer(answerDto, optionsCache)) {
                    createAnomalyFromPVAnswer(
                            answerDto,
                            submissionDto.getUserId(),
                            submissionDto.getDamId(),
                            questionnaireDto.getTemplateQuestionnaireId(),
                            pendingUploads
                    );
                }
            }

            if (questionnaireDto.getOthers() != null) {
                for (OtherSubmissionDTO other : questionnaireDto.getOthers()) {
                    createAnomalyFromOther(other, submissionDto.getUserId(), submissionDto.getDamId(),
                            questionnaireDto.getTemplateQuestionnaireId(), pendingUploads);
                }
            }
        }

        if (submissionDto.getOthers() != null) {
            for (OtherSubmissionDTO other : submissionDto.getOthers()) {
                createAnomalyFromOther(other, submissionDto.getUserId(), submissionDto.getDamId(),
                        null, pendingUploads);
            }
        }

        updateLastAchievementChecklist(submissionDto.getDamId());

        return new ChecklistResponseSubmissionService.SubmissionResult(checklistResponse, pendingUploads);
    }

    @Transactional
    public void updateAnswerPhotoPath(Long entityId, String url) {
        answerPhotoRepository.updateImagePath(entityId, url);
    }

    @Transactional
    public void updateAnomalyPhotoPath(Long entityId, String url) {
        anomalyPhotoRepository.updateImagePath(entityId, url);
    }

    // =========================================================================
    // FLUXO PRESIGNED (direto-pro-S3) — transacional, tudo-ou-nada.
    //
    // As imagens JÁ estão no S3 quando este método roda; recebemos apenas o mapa
    // objectKey → URL final (já validado: prefixo + existência no S3). Aqui só
    // gravamos linhas — se qualquer coisa falhar, ROLLBACK total (nada persiste).
    //
    // Reusa TODOS os validadores privados do fluxo base64 (mesma lógica, sem
    // divergência). O método base64 acima permanece INTACTO. A única diferença é
    // a foto: gravamos imagePath = URL final direto (sem "pending", sem async).
    // =========================================================================
    @Transactional(timeout = 120)
    public ChecklistResponseEntity persistChecklistDataPresigned(
            ChecklistResponseSubmissionDTO submissionDto,
            Map<String, String> urlByObjectKey) {

        validateUserAccessToDam(submissionDto.getUserId(), submissionDto.getDamId());

        if (!AuthenticatedUserUtil.isAdmin()) {

            UserEntity currentUser = AuthenticatedUserUtil.getCurrentUser();

            if (currentUser.getRoutineInspectionPermission() == null) {
                currentUser = userRepository.findByIdWithPermissions(currentUser.getId())
                        .orElseThrow(() -> new NotFoundException("Usuário logado não encontrado"));
            }

            if (currentUser.getRoutineInspectionPermission() == null) {
                throw new ForbiddenException("Usuário não tem permissão para preencher checklist!");
            }

            if (submissionDto.isMobile()) {
                if (!Boolean.TRUE.equals(currentUser.getRoutineInspectionPermission().getIsFillMobile())) {
                    throw new ForbiddenException("Usuário não tem permissão para preencher checklist via mobile!");
                }
            } else {
                if (!Boolean.TRUE.equals(currentUser.getRoutineInspectionPermission().getIsFillWeb())) {
                    throw new ForbiddenException("Usuário não tem permissão para preencher checklist via web!");
                }
            }
        }

        Map<Long, String> optionsCache = loadOptionsCache(submissionDto);

        validateSubmission(submissionDto, optionsCache);

        ChecklistResponseEntity checklistResponse = createChecklistResponse(submissionDto);

        for (QuestionnaireResponseSubmissionDTO questionnaireDto : submissionDto.getQuestionnaireResponses()) {

            QuestionnaireResponseEntity questionnaireResponse = createQuestionnaireResponse(questionnaireDto, checklistResponse);

            for (AnswerSubmissionDTO answerDto : questionnaireDto.getAnswers()) {

                createAnswerPresigned(answerDto, questionnaireResponse, optionsCache, urlByObjectKey);

                if (pvAnswerValidator.isPVAnswer(answerDto, optionsCache)) {
                    createAnomalyFromPVAnswerPresigned(
                            answerDto,
                            submissionDto.getUserId(),
                            submissionDto.getDamId(),
                            questionnaireDto.getTemplateQuestionnaireId(),
                            urlByObjectKey
                    );
                }
            }

            if (questionnaireDto.getOthers() != null) {
                for (OtherSubmissionDTO other : questionnaireDto.getOthers()) {
                    createAnomalyFromOtherPresigned(other, submissionDto.getUserId(), submissionDto.getDamId(),
                            questionnaireDto.getTemplateQuestionnaireId(), urlByObjectKey);
                }
            }
        }

        if (submissionDto.getOthers() != null) {
            for (OtherSubmissionDTO other : submissionDto.getOthers()) {
                createAnomalyFromOtherPresigned(other, submissionDto.getUserId(), submissionDto.getDamId(),
                        null, urlByObjectKey);
            }
        }

        updateLastAchievementChecklist(submissionDto.getDamId());

        return checklistResponse;
    }

    private AnswerEntity createAnswerPresigned(AnswerSubmissionDTO answerDto,
            QuestionnaireResponseEntity questionnaireResponse,
            Map<Long, String> optionsCache, Map<String, String> urlByObjectKey) {
        QuestionEntity question = questionRepository.getReferenceById(answerDto.getQuestionId());

        AnswerEntity answer = new AnswerEntity();
        answer.setQuestion(question);
        answer.setQuestionnaireResponse(questionnaireResponse);
        answer.setLatitude(answerDto.getLatitude());
        answer.setLongitude(answerDto.getLongitude());

        if (answerDto.getComment() != null && !answerDto.getComment().trim().isEmpty()) {
            answer.setComment(answerDto.getComment());
        }

        if (answerDto.getSelectedOptionIds() != null && !answerDto.getSelectedOptionIds().isEmpty()) {
            Set<OptionEntity> options = new HashSet<>();
            for (Long optionId : answerDto.getSelectedOptionIds()) {
                if (!optionsCache.containsKey(optionId)) {
                    throw new NotFoundException("Opção inválida: " + optionId);
                }
                options.add(optionRepository.getReferenceById(optionId));
            }
            answer.setSelectedOptions(options);
        }

        AnswerEntity savedAnswer = answerRepository.save(answer);

        if (answerDto.getPhotos() != null && !answerDto.getPhotos().isEmpty()) {
            for (PhotoSubmissionDTO photoDto : answerDto.getPhotos()) {
                AnswerPhotoEntity photo = new AnswerPhotoEntity();
                photo.setAnswer(savedAnswer);
                photo.setImagePath(resolvePresignedUrl(photoDto, urlByObjectKey));
                answerPhotoRepository.save(photo);
            }
        }

        return savedAnswer;
    }

    private void createAnomalyFromPVAnswerPresigned(AnswerSubmissionDTO answerDto, Long userId, Long damId,
            Long questionnaireId, Map<String, String> urlByObjectKey) {

        UserEntity user = userRepository.getReferenceById(userId);
        DamEntity dam = damRepository.getReferenceById(damId);
        DangerLevelEntity dangerLevel = dangerLevelRepository.getReferenceById(answerDto.getAnomalyDangerLevelId());
        AnomalyStatusEntity status = anomalyStatusRepository.getReferenceById(answerDto.getAnomalyStatusId());

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setUser(user);
        anomaly.setDam(dam);
        anomaly.setLatitude(answerDto.getLatitude());
        anomaly.setLongitude(answerDto.getLongitude());
        anomaly.setQuestionnaireId(questionnaireId);
        anomaly.setQuestionId(answerDto.getQuestionId());
        anomaly.setOrigin(AnomalyOriginEnum.CHECKLIST);
        String pvObservation = answerDto.getComment();
        anomaly.setObservation(pvObservation != null && !pvObservation.trim().isEmpty() ? pvObservation : null);
        String pvRecommendation = answerDto.getAnomalyRecommendation();
        anomaly.setRecommendation(pvRecommendation != null && !pvRecommendation.trim().isEmpty() ? pvRecommendation : null);
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);

        AnomalyEntity savedAnomaly = anomalyRepository.save(anomaly);

        if (answerDto.getPhotos() != null && !answerDto.getPhotos().isEmpty()) {
            for (PhotoSubmissionDTO photoDto : answerDto.getPhotos()) {
                savePresignedAnomalyPhoto(photoDto, savedAnomaly, damId, urlByObjectKey);
            }
        }
    }

    private void createAnomalyFromOtherPresigned(OtherSubmissionDTO otherDto, Long userId, Long damId,
            Long questionnaireId, Map<String, String> urlByObjectKey) {

        UserEntity user = userRepository.getReferenceById(userId);
        DamEntity dam = damRepository.getReferenceById(damId);
        DangerLevelEntity dangerLevel = dangerLevelRepository.getReferenceById(otherDto.getAnomalyDangerLevelId());
        AnomalyStatusEntity status = anomalyStatusRepository.getReferenceById(otherDto.getAnomalyStatusId());

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setUser(user);
        anomaly.setDam(dam);
        anomaly.setLatitude(otherDto.getLatitude());
        anomaly.setLongitude(otherDto.getLongitude());
        anomaly.setQuestionnaireId(questionnaireId);
        anomaly.setQuestionId(null);
        anomaly.setOrigin(AnomalyOriginEnum.CHECKLIST);
        anomaly.setObservation(otherDto.getObservation());
        anomaly.setRecommendation(otherDto.getRecommendation());
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);

        AnomalyEntity saved = anomalyRepository.save(anomaly);

        for (PhotoSubmissionDTO photoDto : otherDto.getPhotos()) {
            savePresignedAnomalyPhoto(photoDto, saved, damId, urlByObjectKey);
        }
    }

    private void savePresignedAnomalyPhoto(PhotoSubmissionDTO photoDto, AnomalyEntity anomaly, Long damId,
            Map<String, String> urlByObjectKey) {
        AnomalyPhotoEntity photoEntity = new AnomalyPhotoEntity();
        photoEntity.setAnomaly(anomaly);
        photoEntity.setImagePath(resolvePresignedUrl(photoDto, urlByObjectKey));
        photoEntity.setDamId(damId);
        anomalyPhotoRepository.save(photoEntity);
    }

    private String resolvePresignedUrl(PhotoSubmissionDTO photoDto, Map<String, String> urlByObjectKey) {
        String url = urlByObjectKey.get(photoDto.getObjectKey());
        if (url == null) {
            throw new InvalidInputException(
                    "Imagem não resolvida para objectKey: " + photoDto.getObjectKey());
        }
        return url;
    }

    /**
     * Limite de fotos por pergunta. "Outros" não têm teto — a especificação é
     * explícita quanto a essa diferença.
     */
    private static final int MAX_PHOTOS_PER_ANSWER = 15;

    /**
     * O app exige observação no NI; este servidor nunca exigiu. Ligar quando as
     * duas pontas estiverem alinhadas — ver
     * ChecklistOptionTransitionValidator.
     */
    @Value("${checklist.validation.require-observation-on-ni:false}")
    private boolean requireObservationOnNi;

    private Map<Long, String> loadOptionsCache(ChecklistResponseSubmissionDTO dto) {
        Set<Long> allOptionIds = collectOptionIds(dto);
        if (allOptionIds.isEmpty()) {
            return Map.of();
        }
        return optionRepository.findAllById(allOptionIds).stream()
                .collect(Collectors.toMap(OptionEntity::getId, OptionEntity::getLabel));
    }

    private Set<Long> collectOptionIds(ChecklistResponseSubmissionDTO dto) {
        Set<Long> ids = new HashSet<>();
        if (dto.getQuestionnaireResponses() != null) {
            for (QuestionnaireResponseSubmissionDTO q : dto.getQuestionnaireResponses()) {
                if (q.getAnswers() != null) {
                    for (AnswerSubmissionDTO a : q.getAnswers()) {
                        if (a.getSelectedOptionIds() != null) {
                            ids.addAll(a.getSelectedOptionIds());
                        }
                    }
                }
            }
        }
        return ids;
    }

    /**
     * Todas as regras de preenchimento, na ordem em que o inspetor precisa
     * vê-las.
     *
     * A ordem não é decorativa. Uma transição inválida é um erro sobre a
     * resposta em si; um campo faltando é um erro sobre o que acompanha a
     * resposta. Reclamar do campo antes da transição faz o inspetor preencher
     * foto e observação para só então descobrir que aquela opção nunca poderia
     * ter sido escolhida.
     *
     * Pelo mesmo motivo, a checagem de perguntas não respondidas vem por último:
     * é o que a UI resolve oferecendo "completar com NE", e não faz sentido
     * oferecer isso enquanto o que já foi respondido está errado.
     *
     * Roda inteira ANTES de qualquer escrita — os dois fluxos (base64 e
     * presigned) chamam este mesmo método, para não voltarem a divergir.
     */
    private void validateSubmission(ChecklistResponseSubmissionDTO submissionDto, Map<Long, String> optionsCache) {

        validateAllRequiredQuestionnaires(submissionDto);

        List<Long> allQuestionIds = submissionDto.getQuestionnaireResponses().stream()
                .flatMap(q -> q.getAnswers().stream())
                .map(AnswerSubmissionDTO::getQuestionId)
                .collect(Collectors.toList());

        Map<Long, QuestionEntity> questions = questionRepository.findAllById(allQuestionIds).stream()
                .collect(Collectors.toMap(QuestionEntity::getId, q -> q, (a, b) -> a));

        Map<String, String> previousLabels = loadPreviousLabels(submissionDto, allQuestionIds);

        for (QuestionnaireResponseSubmissionDTO qDto : submissionDto.getQuestionnaireResponses()) {
            for (AnswerSubmissionDTO answerDto : qDto.getAnswers()) {
                validateAnswerTransition(answerDto, qDto.getTemplateQuestionnaireId(),
                        previousLabels, questions, optionsCache);
            }
        }

        for (QuestionnaireResponseSubmissionDTO qDto : submissionDto.getQuestionnaireResponses()) {
            for (AnswerSubmissionDTO answerDto : qDto.getAnswers()) {
                validateAnswerFields(answerDto, questions, optionsCache);
            }
        }

        validateOthersHaveRequiredFields(submissionDto);

        for (QuestionnaireResponseSubmissionDTO qDto : submissionDto.getQuestionnaireResponses()) {
            validateAllQuestionsAnswered(qDto);
        }
    }

    private Map<String, String> loadPreviousLabels(
            ChecklistResponseSubmissionDTO submissionDto, List<Long> allQuestionIds) {

        List<Long> allTemplateIds = submissionDto.getQuestionnaireResponses().stream()
                .map(QuestionnaireResponseSubmissionDTO::getTemplateQuestionnaireId)
                .collect(Collectors.toList());

        Map<String, String> previousLabels = new java.util.HashMap<>();

        if (allQuestionIds.isEmpty() || allTemplateIds.isEmpty()) {
            return previousLabels;
        }

        List<Object[]> rows = answerRepository.findLastRelevantOptionLabels(
                allQuestionIds, allTemplateIds, submissionDto.getDamId());

        for (Object[] row : rows) {
            Long questionId = ((Number) row[0]).longValue();
            Long templateId = ((Number) row[1]).longValue();
            previousLabels.put(questionId + "_" + templateId, (String) row[2]);
        }

        return previousLabels;
    }

    private void validateAnswerTransition(
            AnswerSubmissionDTO answerDto,
            Long templateId,
            Map<String, String> previousLabels,
            Map<Long, QuestionEntity> questions,
            Map<Long, String> optionsCache) {

        if (answerDto.getSelectedOptionIds() == null || answerDto.getSelectedOptionIds().isEmpty()) {
            return;
        }

        String questionText = questionText(answerDto.getQuestionId(), questions);
        String previousLabel = previousLabels.get(answerDto.getQuestionId() + "_" + templateId);

        for (Long optionId : answerDto.getSelectedOptionIds()) {
            String newLabel = optionsCache.get(optionId);
            if (newLabel != null) {
                ChecklistOptionTransitionValidator.validateTransition(previousLabel, newLabel, questionText);
            }
        }
    }

    /**
     * Forma da resposta (uma opção por pergunta, teto de fotos) e campos que a
     * opção escolhida torna obrigatórios.
     */
    private void validateAnswerFields(
            AnswerSubmissionDTO answerDto,
            Map<Long, QuestionEntity> questions,
            Map<Long, String> optionsCache) {

        String questionText = questionText(answerDto.getQuestionId(), questions);

        if (answerDto.getPhotos() != null && answerDto.getPhotos().size() > MAX_PHOTOS_PER_ANSWER) {
            throw new InvalidInputException(
                    "A pergunta '" + questionText + "' tem " + answerDto.getPhotos().size()
                    + " fotos. O limite é de " + MAX_PHOTOS_PER_ANSWER + " por pergunta.");
        }

        QuestionEntity question = questions.get(answerDto.getQuestionId());
        boolean isTextQuestion = question != null && TypeQuestionEnum.TEXT.equals(question.getType());

        List<Long> selected = answerDto.getSelectedOptionIds();

        if (isTextQuestion) {
            if (selected != null && !selected.isEmpty()) {
                throw new InvalidInputException(
                        "A pergunta '" + questionText + "' é do tipo texto e não aceita opções selecionadas.");
            }
            if (answerDto.getComment() == null || answerDto.getComment().isBlank()) {
                throw new InvalidInputException(
                        "A pergunta '" + questionText + "' é do tipo texto e exige o campo de texto preenchido.");
            }
            return;
        }

        if (selected == null || selected.isEmpty()) {
            throw new InvalidInputException(
                    "A pergunta '" + questionText + "' não foi respondida: é obrigatório escolher uma opção.");
        }

        if (selected.size() > 1) {
            throw new InvalidInputException(
                    "A pergunta '" + questionText + "' recebeu " + selected.size()
                    + " opções. Cada pergunta aceita exatamente uma.");
        }

        String label = optionsCache.get(selected.get(0));
        if (label == null) {
            throw new NotFoundException("Opção inválida: " + selected.get(0));
        }

        boolean hasPhotos = answerDto.getPhotos() != null && !answerDto.getPhotos().isEmpty();
        boolean hasLocation = answerDto.getLatitude() != null && answerDto.getLongitude() != null;

        ChecklistOptionTransitionValidator.validateAnswerFields(
                label,
                answerDto.getComment(),
                hasPhotos,
                hasLocation,
                answerDto.getAnomalyRecommendation(),
                answerDto.getAnomalyDangerLevelId(),
                answerDto.getAnomalyStatusId(),
                requireObservationOnNi,
                questionText);
    }

    private String questionText(Long questionId, Map<Long, QuestionEntity> questions) {
        QuestionEntity question = questions.get(questionId);
        return question != null ? question.getQuestionText() : "Desconhecida";
    }

    private void updateLastAchievementChecklist(Long damId) {
        documentationDamRepository.findByDamId(damId).ifPresent(documentationDam -> {
            documentationDam.setLastAchievementChecklist(LocalDate.now());
            documentationDamRepository.save(documentationDam);
        });
    }

    private void validateUserAccessToDam(Long userId, Long damId) {
        UserEntity user = userRepository.findByIdWithPermissions(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        DamEntity dam = damRepository.findById(damId)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));

        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }

        boolean userBelongsToClient = user.getClients().stream()
                .anyMatch(client -> client.getId().equals(dam.getClient().getId()));

        if (!userBelongsToClient) {
            throw new ForbiddenException(
                    "Usuário não tem permissão para acessar esta barragem. "
                    + "O usuário não pertence ao cliente proprietário da barragem."
            );
        }

        boolean hasSpecificPermission = user.getDamPermissions().stream()
                .anyMatch(permission
                        -> permission.getDam().getId().equals(damId)
                && Boolean.TRUE.equals(permission.getHasAccess())
                && permission.getClient().getId().equals(dam.getClient().getId())
                );

        if (!hasSpecificPermission) {
            throw new ForbiddenException(
                    "Usuário não tem permissão específica para acessar esta barragem. "
                    + "Verifique as permissões de acesso na administração do sistema."
            );
        }
    }

    private void createAnomalyFromPVAnswer(
            AnswerSubmissionDTO answerDto,
            Long userId,
            Long damId,
            Long questionnaireId,
            List<ChecklistResponseSubmissionService.PendingPhotoUpload> pendingUploads) {

        UserEntity user = userRepository.getReferenceById(userId);
        DamEntity dam = damRepository.getReferenceById(damId);
        DangerLevelEntity dangerLevel = dangerLevelRepository.getReferenceById(answerDto.getAnomalyDangerLevelId());
        AnomalyStatusEntity status = anomalyStatusRepository.getReferenceById(answerDto.getAnomalyStatusId());

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setUser(user);
        anomaly.setDam(dam);
        anomaly.setLatitude(answerDto.getLatitude());
        anomaly.setLongitude(answerDto.getLongitude());
        anomaly.setQuestionnaireId(questionnaireId);
        anomaly.setQuestionId(answerDto.getQuestionId());
        anomaly.setOrigin(AnomalyOriginEnum.CHECKLIST);
        String pvObservation = answerDto.getComment();
        anomaly.setObservation(pvObservation != null && !pvObservation.trim().isEmpty() ? pvObservation : null);
        String pvRecommendation = answerDto.getAnomalyRecommendation();
        anomaly.setRecommendation(pvRecommendation != null && !pvRecommendation.trim().isEmpty() ? pvRecommendation : null);
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);

        AnomalyEntity savedAnomaly = anomalyRepository.save(anomaly);

        if (answerDto.getPhotos() != null && !answerDto.getPhotos().isEmpty()) {
            for (PhotoSubmissionDTO photoDto : answerDto.getPhotos()) {
                prepareAnomalyPhoto(photoDto, savedAnomaly, damId, pendingUploads);
            }
        }
    }

    private void prepareAnomalyPhoto(PhotoSubmissionDTO photoDto, AnomalyEntity anomaly, Long damId,
            List<ChecklistResponseSubmissionService.PendingPhotoUpload> pendingUploads) {
        String base64Image = photoDto.getBase64Image();
        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        AnomalyPhotoEntity photoEntity = new AnomalyPhotoEntity();
        photoEntity.setAnomaly(anomaly);
        photoEntity.setImagePath("pending");
        photoEntity.setDamId(damId);
        AnomalyPhotoEntity saved = anomalyPhotoRepository.save(photoEntity);

        pendingUploads.add(new ChecklistResponseSubmissionService.PendingPhotoUpload(
                saved.getId(), true, imageBytes,
                photoDto.getFileName(), photoDto.getContentType(), "anomalies", damId));
    }

    /**
     * "Outros" — ocorrência sem pergunta associada. Exige a mesma tríade do PV:
     * observação, ao menos uma foto e localização.
     *
     * As anotações de bean validation no DTO já cobrem isso quando a requisição
     * entra pelo controller. A checagem é repetida aqui porque este serviço é o
     * ponto por onde os dois fluxos passam, e uma ocorrência sem foto ou sem
     * coordenada vira uma anomalia que ninguém consegue localizar em campo.
     */
    private void validateOthersHaveRequiredFields(ChecklistResponseSubmissionDTO submissionDto) {
        for (QuestionnaireResponseSubmissionDTO qDto : submissionDto.getQuestionnaireResponses()) {
            if (qDto.getOthers() == null) {
                continue;
            }
            for (OtherSubmissionDTO other : qDto.getOthers()) {
                validateOther(other);
            }
        }
        if (submissionDto.getOthers() != null) {
            for (OtherSubmissionDTO other : submissionDto.getOthers()) {
                validateOther(other);
            }
        }
    }

    private void validateOther(OtherSubmissionDTO other) {
        List<String> missing = new ArrayList<>();

        if (other.getObservation() == null || other.getObservation().isBlank()) {
            missing.add("Observação");
        }
        if (other.getPhotos() == null || other.getPhotos().isEmpty()) {
            missing.add("Foto");
        }
        if (other.getLatitude() == null || other.getLongitude() == null) {
            missing.add("Localização");
        }

        if (!missing.isEmpty()) {
            throw new InvalidInputException(
                    "Ocorrência em 'Outros' exige obrigatoriamente: " + String.join(", ", missing) + ".");
        }
    }

    private void createAnomalyFromOther(
            OtherSubmissionDTO otherDto,
            Long userId,
            Long damId,
            Long questionnaireId,
            List<ChecklistResponseSubmissionService.PendingPhotoUpload> pendingUploads) {

        UserEntity user = userRepository.getReferenceById(userId);
        DamEntity dam = damRepository.getReferenceById(damId);
        DangerLevelEntity dangerLevel = dangerLevelRepository.getReferenceById(otherDto.getAnomalyDangerLevelId());
        AnomalyStatusEntity status = anomalyStatusRepository.getReferenceById(otherDto.getAnomalyStatusId());

        AnomalyEntity anomaly = new AnomalyEntity();
        anomaly.setUser(user);
        anomaly.setDam(dam);
        anomaly.setLatitude(otherDto.getLatitude());
        anomaly.setLongitude(otherDto.getLongitude());
        anomaly.setQuestionnaireId(questionnaireId);
        anomaly.setQuestionId(null);
        anomaly.setOrigin(AnomalyOriginEnum.CHECKLIST);
        anomaly.setObservation(otherDto.getObservation());
        anomaly.setRecommendation(otherDto.getRecommendation());
        anomaly.setDangerLevel(dangerLevel);
        anomaly.setStatus(status);

        AnomalyEntity saved = anomalyRepository.save(anomaly);

        for (PhotoSubmissionDTO photoDto : otherDto.getPhotos()) {
            prepareAnomalyPhoto(photoDto, saved, damId, pendingUploads);
        }
    }

    private void validateAllRequiredQuestionnaires(ChecklistResponseSubmissionDTO submissionDto) {
        ChecklistEntity checklist = checklistRepository.findByIdWithFullDetails(submissionDto.getChecklistId())
                .orElseThrow(() -> new NotFoundException("Checklist não encontrado: " + submissionDto.getChecklistId()));

        Set<Long> requiredTemplateIds = checklist.getChecklistTemplates().stream()
                .map(ct -> ct.getTemplateQuestionnaire().getId())
                .collect(Collectors.toSet());

        List<Long> submittedTemplateIdsList = submissionDto.getQuestionnaireResponses().stream()
                .map(QuestionnaireResponseSubmissionDTO::getTemplateQuestionnaireId)
                .collect(Collectors.toList());

        Set<Long> submittedTemplateIdsSet = new HashSet<>(submittedTemplateIdsList);

        if (submittedTemplateIdsSet.size() < submittedTemplateIdsList.size()) {
            throw new InvalidInputException("Existem questionários duplicados na submissão.");
        }

        if (!submittedTemplateIdsSet.containsAll(requiredTemplateIds)) {
            Set<Long> missingTemplateIds = new HashSet<>(requiredTemplateIds);
            missingTemplateIds.removeAll(submittedTemplateIdsSet);

            Set<Long> missingWithQuestions = templateQuestionnaireRepository.findIdsWithQuestions(missingTemplateIds);
            if (!missingWithQuestions.isEmpty()) {
                List<String> missingNames = templateQuestionnaireRepository.findAllById(missingWithQuestions).stream()
                        .map(TemplateQuestionnaireEntity::getName).toList();
                throw new InvalidInputException("Checklist incompleto. Faltam: " + missingNames);
            }
        }

        if (!requiredTemplateIds.containsAll(submittedTemplateIdsSet)) {
            Set<Long> extraTemplateIds = new HashSet<>(submittedTemplateIdsSet);
            extraTemplateIds.removeAll(requiredTemplateIds);
            throw new InvalidInputException("Questionários extras enviados: " + extraTemplateIds);
        }
    }

    private void validateAllQuestionsAnswered(QuestionnaireResponseSubmissionDTO questionnaireDto) {
        TemplateQuestionnaireEntity template = templateQuestionnaireRepository
                .findByIdWithFullDetails(questionnaireDto.getTemplateQuestionnaireId())
                .orElseThrow(() -> new NotFoundException("Template não encontrado"));

        Set<Long> expectedQuestionIds = template.getTemplateQuestions().stream()
                .map(tq -> tq.getQuestion().getId())
                .collect(Collectors.toSet());

        List<Long> submittedQuestionIdsList = questionnaireDto.getAnswers().stream()
                .map(AnswerSubmissionDTO::getQuestionId)
                .collect(Collectors.toList());

        Set<Long> uniqueSubmittedIds = new HashSet<>(submittedQuestionIdsList);

        if (uniqueSubmittedIds.size() < submittedQuestionIdsList.size()) {
            throw new InvalidInputException("Respostas duplicadas no questionário: " + template.getName());
        }

        if (!uniqueSubmittedIds.containsAll(expectedQuestionIds)) {
            Set<Long> missingIds = new HashSet<>(expectedQuestionIds);
            missingIds.removeAll(uniqueSubmittedIds);

            List<String> missingTexts = questionRepository.findAllById(missingIds).stream()
                    .map(QuestionEntity::getQuestionText).toList();

            throw new InvalidInputException("Perguntas não respondidas em '" + template.getName() + "': " + missingTexts);
        }

        if (!expectedQuestionIds.containsAll(uniqueSubmittedIds)) {
            Set<Long> extraIds = new HashSet<>(uniqueSubmittedIds);
            extraIds.removeAll(expectedQuestionIds);
            throw new InvalidInputException("Perguntas extras em '" + template.getName() + "': " + extraIds);
        }
    }

    private ChecklistResponseEntity createChecklistResponse(ChecklistResponseSubmissionDTO submissionDto) {
        DamEntity dam = damService.findById(submissionDto.getDamId());
        UserEntity user = userRepository.getReferenceById(submissionDto.getUserId());

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
        checklistResponse.setStartedAt(submissionDto.getStartedAt());
        checklistResponse.setFinishedAt(submissionDto.getFinishedAt());

        return checklistResponseRepository.save(checklistResponse);
    }

    private Double formatToTwoDecimals(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private QuestionnaireResponseEntity createQuestionnaireResponse(
            QuestionnaireResponseSubmissionDTO questionnaireDto,
            ChecklistResponseEntity checklistResponse) {

        TemplateQuestionnaireEntity templateQuestionnaire = templateQuestionnaireRepository
                .getReferenceById(questionnaireDto.getTemplateQuestionnaireId());

        QuestionnaireResponseEntity questionnaireResponse = new QuestionnaireResponseEntity();
        questionnaireResponse.setTemplateQuestionnaire(templateQuestionnaire);
        questionnaireResponse.setChecklistResponse(checklistResponse);
        questionnaireResponse.setDam(checklistResponse.getDam());

        return questionnaireResponseRepository.save(questionnaireResponse);
    }

    private AnswerEntity createAnswer(AnswerSubmissionDTO answerDto, QuestionnaireResponseEntity questionnaireResponse,
            Map<Long, String> optionsCache, List<ChecklistResponseSubmissionService.PendingPhotoUpload> pendingUploads) {
        QuestionEntity question = questionRepository.getReferenceById(answerDto.getQuestionId());

        AnswerEntity answer = new AnswerEntity();
        answer.setQuestion(question);
        answer.setQuestionnaireResponse(questionnaireResponse);
        answer.setLatitude(answerDto.getLatitude());
        answer.setLongitude(answerDto.getLongitude());

        if (answerDto.getComment() != null && !answerDto.getComment().trim().isEmpty()) {
            answer.setComment(answerDto.getComment());
        }

        if (answerDto.getSelectedOptionIds() != null && !answerDto.getSelectedOptionIds().isEmpty()) {
            Set<OptionEntity> options = new HashSet<>();
            for (Long optionId : answerDto.getSelectedOptionIds()) {
                if (!optionsCache.containsKey(optionId)) {
                    throw new NotFoundException("Opção inválida: " + optionId);
                }
                options.add(optionRepository.getReferenceById(optionId));
            }
            answer.setSelectedOptions(options);
        }

        AnswerEntity savedAnswer = answerRepository.save(answer);

        if (answerDto.getPhotos() != null && !answerDto.getPhotos().isEmpty()) {
            for (PhotoSubmissionDTO photoDto : answerDto.getPhotos()) {
                prepareAnswerPhoto(photoDto, savedAnswer, pendingUploads);
            }
        }

        return savedAnswer;
    }

    private void prepareAnswerPhoto(PhotoSubmissionDTO photoDto, AnswerEntity answer,
            List<ChecklistResponseSubmissionService.PendingPhotoUpload> pendingUploads) {
        String base64Image = photoDto.getBase64Image();
        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        AnswerPhotoEntity photo = new AnswerPhotoEntity();
        photo.setAnswer(answer);
        photo.setImagePath("pending");
        AnswerPhotoEntity saved = answerPhotoRepository.save(photo);

        pendingUploads.add(new ChecklistResponseSubmissionService.PendingPhotoUpload(
                saved.getId(), false, imageBytes,
                photoDto.getFileName(), photoDto.getContentType(), "answer-photos", null));
    }
}
