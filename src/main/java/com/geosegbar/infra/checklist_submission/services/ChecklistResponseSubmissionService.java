package com.geosegbar.infra.checklist_submission.services;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.geosegbar.common.enums.TypeQuestionEnum;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.AnswerPhotoEntity;
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.answer_photo.persistence.jpa.AnswerPhotoRepository;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.ChecklistResponseSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;
import com.geosegbar.infra.checklist_submission.dtos.QuestionnaireResponseSubmissionDTO;
import com.geosegbar.infra.dam.services.DamService;
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


@Transactional
public ChecklistResponseEntity submitChecklistResponse(ChecklistResponseSubmissionDTO submissionDto) {
    ChecklistResponseEntity checklistResponse = createChecklistResponse(submissionDto);
    
    validateAllRequiredQuestionnaires(submissionDto);
    
    for (QuestionnaireResponseSubmissionDTO questionnaireDto : submissionDto.getQuestionnaireResponses()) {
        validateAllQuestionsAnswered(questionnaireDto);
        
        QuestionnaireResponseEntity questionnaireResponse = createQuestionnaireResponse(questionnaireDto, checklistResponse);
        
        for (AnswerSubmissionDTO answerDto : questionnaireDto.getAnswers()) {
            createAnswer(answerDto, questionnaireResponse);
        }
    }
    
    return checklistResponse;
}

private void validateAllRequiredQuestionnaires(ChecklistResponseSubmissionDTO submissionDto) {
    ChecklistEntity checklist = checklistRepository.findByNameIgnoreCase(submissionDto.getChecklistName())
        .orElseThrow(() -> new NotFoundException("Checklist não encontrado com o nome: " + submissionDto.getChecklistName()));
    
    Set<TemplateQuestionnaireEntity> requiredTemplates = checklist.getTemplateQuestionnaires();
    
    Set<Long> requiredTemplateIds = requiredTemplates.stream()
        .map(TemplateQuestionnaireEntity::getId)
        .collect(Collectors.toSet());
    
    Set<Long> submittedTemplateIds = submissionDto.getQuestionnaireResponses().stream()
        .map(QuestionnaireResponseSubmissionDTO::getTemplateQuestionnaireId)
        .collect(Collectors.toSet());
    
    if (!submittedTemplateIds.containsAll(requiredTemplateIds)) {
        Set<Long> missingTemplateIds = new HashSet<>(requiredTemplateIds);
        missingTemplateIds.removeAll(submittedTemplateIds);
        
        List<String> missingTemplateNames = missingTemplateIds.stream()
            .map(id -> {
                TemplateQuestionnaireEntity template = templateQuestionnaireRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Template de questionário não encontrado: " + id));
                return template.getName();
            })
            .collect(Collectors.toList());
        
        String errorMsg = String.format(
            "Os seguintes questionários obrigatórios não foram incluídos no checklist '%s': %s",
            checklist.getName(),
            String.join(", ", missingTemplateNames)
        );
        
        throw new InvalidInputException(errorMsg);
    }
    
    if (!requiredTemplateIds.containsAll(submittedTemplateIds)) {
        Set<Long> extraTemplateIds = new HashSet<>(submittedTemplateIds);
        extraTemplateIds.removeAll(requiredTemplateIds);
        
        String errorMsg = String.format(
            "Os seguintes questionários não pertencem ao checklist '%s': %s",
            checklist.getName(),
            extraTemplateIds
        );
        
        throw new InvalidInputException(errorMsg);
    }
}

private void validateAllQuestionsAnswered(QuestionnaireResponseSubmissionDTO questionnaireDto) {
    TemplateQuestionnaireEntity template = templateQuestionnaireRepository
        .findById(questionnaireDto.getTemplateQuestionnaireId())
        .orElseThrow(() -> new NotFoundException("Modelo de questionário não encontrado: " + 
                                                questionnaireDto.getTemplateQuestionnaireId()));
    
    Set<Long> templateQuestionIds = template.getTemplateQuestions().stream()
        .map(tq -> tq.getQuestion().getId())
        .collect(Collectors.toSet());
    
    Set<Long> answeredQuestionIds = questionnaireDto.getAnswers().stream()
        .map(AnswerSubmissionDTO::getQuestionId)
        .collect(Collectors.toSet());
    
    if (!answeredQuestionIds.containsAll(templateQuestionIds)) {
        Set<Long> missingQuestionIds = new HashSet<>(templateQuestionIds);
        missingQuestionIds.removeAll(answeredQuestionIds);
        
        List<String> missingQuestionTexts = missingQuestionIds.stream()
            .map(id -> {
                QuestionEntity question = questionRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Pergunta não encontrada: " + id));
                return question.getQuestionText();
            })
            .collect(Collectors.toList());
        
        String errorMsg = String.format(
            "As seguintes perguntas não foram respondidas para o questionário '%s': %s",
            template.getName(),
            String.join(", ", missingQuestionTexts)
        );
        
        throw new InvalidInputException(errorMsg);
    }
    
    if (!templateQuestionIds.containsAll(answeredQuestionIds)) {
        Set<Long> extraQuestionIds = new HashSet<>(answeredQuestionIds);
        extraQuestionIds.removeAll(templateQuestionIds);
        
        String errorMsg = String.format(
            "As seguintes perguntas não pertencem ao questionário '%s': %s",
            template.getName(),
            extraQuestionIds
        );
        
        throw new InvalidInputException(errorMsg);
    }
}
    
private ChecklistResponseEntity createChecklistResponse(ChecklistResponseSubmissionDTO submissionDto) {
    DamEntity dam = damService.findById(submissionDto.getDamId());
    UserEntity user = userRepository.findById(submissionDto.getUserId())
        .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

    ChecklistResponseEntity checklistResponse = new ChecklistResponseEntity();
    checklistResponse.setChecklistName(submissionDto.getChecklistName());
    checklistResponse.setDam(dam);
    checklistResponse.setUser(user);

    return checklistResponseRepository.save(checklistResponse);
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
    
private AnswerEntity createAnswer(AnswerSubmissionDTO answerDto, QuestionnaireResponseEntity questionnaireResponse) {
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
            throw new InvalidInputException("A pergunta '" + question.getQuestionText() + 
                "' é do tipo TEXT e requer uma resposta textual!");
        }
        answer.setComment(answerDto.getComment());
    } else if (TypeQuestionEnum.CHECKBOX.equals(question.getType())) {
        if (answerDto.getSelectedOptionIds() == null || answerDto.getSelectedOptionIds().isEmpty()) {
            throw new InvalidInputException("A pergunta '" + question.getQuestionText() + 
                "' é do tipo CHECKBOX e requer ao menos uma opção selecionada!");
        }
        
        Set<OptionEntity> options = new HashSet<>();
        for (Long optionId : answerDto.getSelectedOptionIds()) {
            OptionEntity option = optionRepository
                .findById(optionId)
                .orElseThrow(() -> new NotFoundException("Opção não encontrada: " + optionId));
            options.add(option);
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