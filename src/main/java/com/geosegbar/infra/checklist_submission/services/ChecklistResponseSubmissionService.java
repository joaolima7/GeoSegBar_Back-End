package com.geosegbar.infra.checklist_submission.services;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.AnswerPhotoEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.exceptions.FileStorageException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.answer_photo.persistence.jpa.AnswerPhotoRepository;
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

    @Transactional
    public ChecklistResponseEntity submitChecklistResponse(ChecklistResponseSubmissionDTO submissionDto) {
        // 1. Criar a ChecklistResponse
        ChecklistResponseEntity checklistResponse = createChecklistResponse(submissionDto);
        
        // 2. Processar cada questionário
        for (QuestionnaireResponseSubmissionDTO questionnaireDto : submissionDto.getQuestionnaireResponses()) {
            QuestionnaireResponseEntity questionnaireResponse = createQuestionnaireResponse(questionnaireDto, checklistResponse);
            
            // 3. Processar cada resposta do questionário
            for (AnswerSubmissionDTO answerDto : questionnaireDto.getAnswers()) {
                createAnswer(answerDto, questionnaireResponse);
            }
        }
        
        // 4. Retornar a ChecklistResponse salva com todas as relações
        return checklistResponse;
    }
    
    private ChecklistResponseEntity createChecklistResponse(ChecklistResponseSubmissionDTO submissionDto) {
        DamEntity dam = damService.findById(submissionDto.getDamId());
        
        ChecklistResponseEntity checklistResponse = new ChecklistResponseEntity();
        checklistResponse.setChecklistName(submissionDto.getChecklistName());
        checklistResponse.setDam(dam);
        
        return checklistResponseRepository.save(checklistResponse);
    }
    
    private QuestionnaireResponseEntity createQuestionnaireResponse(
            QuestionnaireResponseSubmissionDTO questionnaireDto, 
            ChecklistResponseEntity checklistResponse) {
        
        TemplateQuestionnaireEntity templateQuestionnaire = templateQuestionnaireRepository
            .findById(questionnaireDto.getTemplateQuestionnaireId())
            .orElseThrow(() -> new NotFoundException("Modelo de questionário não encontrado: " + questionnaireDto.getTemplateQuestionnaireId()));
        
        QuestionnaireResponseEntity questionnaireResponse = new QuestionnaireResponseEntity();
        questionnaireResponse.setTemplateQuestionnaire(templateQuestionnaire);
        questionnaireResponse.setUserId(questionnaireDto.getUserId());
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
        answer.setComment(answerDto.getComment());
        answer.setLatitude(answerDto.getLatitude());
        answer.setLongitude(answerDto.getLongitude());
        
        // Processar opções selecionadas
        if (answerDto.getSelectedOptionIds() != null && !answerDto.getSelectedOptionIds().isEmpty()) {
            Set<OptionEntity> options = new HashSet<>();
            for (Long optionId : answerDto.getSelectedOptionIds()) {
                OptionEntity option = optionRepository
                    .findById(optionId)
                    .orElseThrow(() -> new NotFoundException("Opção não encontrada: " + optionId));
                options.add(option);
            }
            answer.setSelectedOptions(options);
        }
        
        // Salvar a resposta para obter o ID
        AnswerEntity savedAnswer = answerRepository.save(answer);
        
        // Processar fotos se houver
        if (answerDto.getPhotos() != null && !answerDto.getPhotos().isEmpty()) {
            for (PhotoSubmissionDTO photoDto : answerDto.getPhotos()) {
                saveAnswerPhoto(photoDto, savedAnswer);
            }
        }
        
        return savedAnswer;
    }
    
    private AnswerPhotoEntity saveAnswerPhoto(PhotoSubmissionDTO photoDto, AnswerEntity answer) {
        try {
            // Converter Base64 para bytes
            byte[] imageBytes = Base64.getDecoder().decode(photoDto.getBase64Image());
            
            // Modificar o FileStorageService para aceitar bytes diretamente
            String photoUrl = fileStorageService.storeFileFromBytes(
                imageBytes, 
                photoDto.getFileName(), 
                photoDto.getContentType(), 
                "answer-photos"
            );
            
            // Criar e salvar a entidade de foto
            AnswerPhotoEntity photo = new AnswerPhotoEntity();
            photo.setAnswer(answer);
            photo.setImagePath(photoUrl);
            
            return answerPhotoRepository.save(photo);
            
        } catch (Exception e) {
            throw new FileStorageException("Erro ao processar imagem: " + e.getMessage());
        }
    }
}