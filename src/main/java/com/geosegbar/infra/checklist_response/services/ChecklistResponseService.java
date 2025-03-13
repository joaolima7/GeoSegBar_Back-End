package com.geosegbar.infra.checklist_response.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.AnswerPhotoEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer_photo.persistence.jpa.AnswerPhotoRepository;
import com.geosegbar.infra.checklist_response.dtos.ChecklistResponseDetailDTO;
import com.geosegbar.infra.checklist_response.dtos.OptionInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.PhotoInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.QuestionWithAnswerDTO;
import com.geosegbar.infra.checklist_response.dtos.TemplateWithAnswersDTO;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChecklistResponseService {

    private final ChecklistResponseRepository checklistResponseRepository;
    private final QuestionnaireResponseRepository questionnaireResponseRepository;
    private final AnswerPhotoRepository answerPhotoRepository;
    private final DamService damService;

    public List<ChecklistResponseEntity> findAll() {
        return checklistResponseRepository.findAll();
    }

    public ChecklistResponseEntity findById(Long id) {
        return checklistResponseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para id: " + id));
    }

    public List<ChecklistResponseEntity> findByDamId(Long damId) {
        damService.findById(damId);
        List<ChecklistResponseEntity> responses = checklistResponseRepository.findByDamId(damId);
        if (responses.isEmpty()) {
            throw new NotFoundException("Nenhuma resposta de checklist encontrada para a Barragem com id: " + damId);
        }
        return responses;
    }

    @Transactional
    public ChecklistResponseEntity save(ChecklistResponseEntity checklistResponse) {
        Long damId = checklistResponse.getDam().getId();
        DamEntity dam = damService.findById(damId);
        checklistResponse.setDam(dam);
        
        return checklistResponseRepository.save(checklistResponse);
    }

    @Transactional
    public ChecklistResponseEntity update(ChecklistResponseEntity checklistResponse) {
        checklistResponseRepository.findById(checklistResponse.getId())
            .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para atualização!"));
        
        Long damId = checklistResponse.getDam().getId();
        DamEntity dam = damService.findById(damId);
        checklistResponse.setDam(dam);
        
        return checklistResponseRepository.save(checklistResponse);
    }

    @Transactional
    public void deleteById(Long id) {
        checklistResponseRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para exclusão!"));
        checklistResponseRepository.deleteById(id);
    }


    // Método para buscar checklists respondidos por uma dam
    public List<ChecklistResponseDetailDTO> findChecklistResponsesByDamId(Long damId) {
        // Verifica se a barragem existe
        DamEntity dam = damService.findById(damId);
        
        // Busca todas as respostas de checklist associadas à barragem
        List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository.findByDamId(damId);
        if (checklistResponses.isEmpty()) {
            throw new NotFoundException("Nenhuma resposta de checklist encontrada para a Barragem: " + dam.getName());
        }
        
        // Converte cada resposta de checklist para o formato desejado
        return checklistResponses.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }
    
    // Método para buscar um checklist respondido específico
    public ChecklistResponseDetailDTO findChecklistResponseById(Long checklistResponseId) {
        ChecklistResponseEntity checklistResponse = findById(checklistResponseId);
        return convertToDetailDto(checklistResponse);
    }
    
    // Método para converter a entidade no DTO estruturado
    private ChecklistResponseDetailDTO convertToDetailDto(ChecklistResponseEntity checklistResponse) {
        ChecklistResponseDetailDTO dto = new ChecklistResponseDetailDTO();
        dto.setId(checklistResponse.getId());
        dto.setChecklistName(checklistResponse.getChecklistName());
        dto.setCreatedAt(checklistResponse.getCreatedAt());
        dto.setUserId(checklistResponse.getUser().getId());
        dto.setUserName(checklistResponse.getUser().getName());
        
        // Buscar questionários respondidos para este checklist
        List<QuestionnaireResponseEntity> questionnaireResponses = questionnaireResponseRepository
                .findByChecklistResponseId(checklistResponse.getId());
        
        // Caso não encontre pela referência direta, tenta buscar pelo damId (compatibilidade)
        if (questionnaireResponses == null || questionnaireResponses.isEmpty()) {
            questionnaireResponses = questionnaireResponseRepository
                    .findByDamIdAndCreatedAtBetween(
                            checklistResponse.getDam().getId(),
                            checklistResponse.getCreatedAt().minusMinutes(5),
                            checklistResponse.getCreatedAt().plusHours(1)
                    );
        }
        
        // Mapa para agrupar questionários por templateId
        Map<Long, TemplateWithAnswersDTO> templateMap = new HashMap<>();
        
        // Processa cada questionário respondido
        for (QuestionnaireResponseEntity qResponse : questionnaireResponses) {
            TemplateQuestionnaireEntity template = qResponse.getTemplateQuestionnaire();
            Long templateId = template.getId();
            
            // Cria ou recupera o DTO de template
            TemplateWithAnswersDTO templateDto = templateMap.computeIfAbsent(templateId, 
                    id -> new TemplateWithAnswersDTO(
                            templateId, 
                            template.getName(),
                            qResponse.getId(),
                            new ArrayList<>()   
                    ));
            
            // Processa as respostas
            for (AnswerEntity answer : qResponse.getAnswers()) {
                QuestionEntity question = answer.getQuestion();
                
                // Busca todas as opções da pergunta
                List<OptionInfoDTO> allQuestionOptions = question.getOptions().stream()
                    .map(opt -> new OptionInfoDTO(opt.getId(), opt.getLabel()))
                    .collect(Collectors.toList());
                
                // Busca as fotos da resposta
                List<AnswerPhotoEntity> photos = answerPhotoRepository.findByAnswerId(answer.getId());
                List<PhotoInfoDTO> photoDtos = photos.stream()
                    .map(photo -> new PhotoInfoDTO(photo.getId(), photo.getImagePath()))
                    .collect(Collectors.toList());
                
                // Converte as opções selecionadas
                List<OptionInfoDTO> optionDtos = answer.getSelectedOptions().stream()
                    .map(option -> new OptionInfoDTO(option.getId(), option.getLabel()))
                    .collect(Collectors.toList());
                
                // Cria o objeto QuestionWithAnswer
                QuestionWithAnswerDTO questionWithAnswer = new QuestionWithAnswerDTO(
                        question.getId(),
                        question.getQuestionText(),
                        question.getType(),
                        answer.getId(),
                        answer.getComment(),
                        answer.getLatitude(),
                        answer.getLongitude(),
                        optionDtos,  // selectedOptions
                        photoDtos,
                        allQuestionOptions
                );
                            
                templateDto.getQuestionsWithAnswers().add(questionWithAnswer);
            }
        }
        
        // Converte o mapa para lista de templates
        List<TemplateWithAnswersDTO> templates = new ArrayList<>(templateMap.values());
        dto.setTemplates(templates);
        
        return dto;
    }

    public List<ChecklistResponseDetailDTO> findChecklistResponsesByUserId(Long userId) {
        // Busca todas as respostas de checklist associadas ao usuário
        List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository.findByUserId(userId);
        if (checklistResponses.isEmpty()) {
            throw new NotFoundException("Nenhuma resposta de checklist encontrada para o Usuário com id: " + userId);
        }
        
        // Converte cada resposta de checklist para o formato detalhado
        return checklistResponses.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    public List<ChecklistResponseDetailDTO> findChecklistResponsesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
    // Busca todas as respostas de checklist dentro do intervalo de datas
    List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository.findByCreatedAtBetween(startDate, endDate);
    if (checklistResponses.isEmpty()) {
        throw new NotFoundException("Nenhuma resposta de checklist encontrada no período especificado");
    }
    
    // Converte cada resposta de checklist para o formato detalhado
    return checklistResponses.stream()
            .map(this::convertToDetailDto)
            .collect(Collectors.toList());
    }
}