package com.geosegbar.infra.checklist_response.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.geosegbar.infra.checklist_response.dtos.DamInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.OptionInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.PagedChecklistResponseDTO;
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


    public List<ChecklistResponseDetailDTO> findChecklistResponsesByDamId(Long damId) {
        DamEntity dam = damService.findById(damId);
        
        List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository.findByDamId(damId);
        if (checklistResponses.isEmpty()) {
            throw new NotFoundException("Nenhuma resposta de checklist encontrada para a Barragem: " + dam.getName());
        }
        
        return checklistResponses.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }


    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findChecklistResponsesByClientIdPaged(
        Long clientId, Pageable pageable) {

    List<DamEntity> clientDams = damService.findDamsByClientId(clientId);

    if (clientDams.isEmpty()) {
        throw new NotFoundException("Nenhuma barragem encontrada para o Cliente com ID: " + clientId);
    }

    List<Long> damIds = clientDams.stream()
            .map(DamEntity::getId)
            .collect(Collectors.toList());

    Page<ChecklistResponseEntity> page = checklistResponseRepository.findByDamIdIn(damIds, pageable);

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
    
    public ChecklistResponseDetailDTO findChecklistResponseById(Long checklistResponseId) {
        ChecklistResponseEntity checklistResponse = findById(checklistResponseId);
        return convertToDetailDto(checklistResponse);
    }
    
    private ChecklistResponseDetailDTO convertToDetailDto(ChecklistResponseEntity checklistResponse) {
        ChecklistResponseDetailDTO dto = new ChecklistResponseDetailDTO();
        dto.setId(checklistResponse.getId());
        dto.setChecklistName(checklistResponse.getChecklistName());
        dto.setCreatedAt(checklistResponse.getCreatedAt());
        dto.setUserId(checklistResponse.getUser().getId());
        dto.setUserName(checklistResponse.getUser().getName());

        DamEntity dam = checklistResponse.getDam();
        DamInfoDTO damInfo = new DamInfoDTO();
        damInfo.setId(dam.getId());
        damInfo.setName(dam.getName());

        dto.setDam(damInfo);
        
        List<QuestionnaireResponseEntity> questionnaireResponses = questionnaireResponseRepository
                .findByChecklistResponseId(checklistResponse.getId());
        
        if (questionnaireResponses == null || questionnaireResponses.isEmpty()) {
            questionnaireResponses = questionnaireResponseRepository
                    .findByDamIdAndCreatedAtBetween(
                            checklistResponse.getDam().getId(),
                            checklistResponse.getCreatedAt().minusMinutes(5),
                            checklistResponse.getCreatedAt().plusHours(1)
                    );
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
                
                List<AnswerPhotoEntity> photos = answerPhotoRepository.findByAnswerId(answer.getId());
                List<PhotoInfoDTO> photoDtos = photos.stream()
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
        
        List<TemplateWithAnswersDTO> templates = new ArrayList<>(templateMap.values());
        dto.setTemplates(templates);
        
        return dto;
    }

    public List<ChecklistResponseDetailDTO> findChecklistResponsesByUserId(Long userId) {
        List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository.findByUserId(userId);
        if (checklistResponses.isEmpty()) {
            throw new NotFoundException("Nenhuma resposta de checklist encontrada para o Usuário com id: " + userId);
        }
        
        return checklistResponses.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    public List<ChecklistResponseDetailDTO> findChecklistResponsesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
    List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository.findByCreatedAtBetween(startDate, endDate);
    if (checklistResponses.isEmpty()) {
        throw new NotFoundException("Nenhuma resposta de checklist encontrada no período especificado");
    }
    
    return checklistResponses.stream()
            .map(this::convertToDetailDto)
            .collect(Collectors.toList());
    }

    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findChecklistResponsesByDamIdPaged(Long damId, Pageable pageable) {
    damService.findById(damId);
    
    Page<ChecklistResponseEntity> page = checklistResponseRepository.findByDamId(damId, pageable);
    if (page.isEmpty()) {
        throw new NotFoundException("Nenhuma resposta de checklist encontrada para a Barragem com id: " + damId);
    }
    
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

    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findChecklistResponsesByUserIdPaged(Long userId, Pageable pageable) {
        Page<ChecklistResponseEntity> page = checklistResponseRepository.findByUserId(userId, pageable);
        if (page.isEmpty()) {
            throw new NotFoundException("Nenhuma resposta de checklist encontrada para o Usuário com id: " + userId);
        }
        
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

    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findChecklistResponsesByDateRangePaged(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<ChecklistResponseEntity> page = checklistResponseRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        if (page.isEmpty()) {
            throw new NotFoundException("Nenhuma resposta de checklist encontrada no período especificado");
        }
        
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

    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findAllChecklistResponsesPaged(Pageable pageable) {
        Page<ChecklistResponseEntity> page = checklistResponseRepository.findAll(pageable);
        if (page.isEmpty()) {
            throw new NotFoundException("Nenhuma resposta de checklist encontrada");
        }
        
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
}