package com.geosegbar.infra.checklist_response.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist_response.dtos.ChecklistResponseDetailDTO;
import com.geosegbar.infra.checklist_response.dtos.DamInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.DamLastChecklistDTO;
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

        // ✅ SUBSTITUIR todo o método por esta consulta direta:
        Page<ChecklistResponseEntity> page = checklistResponseRepository.findByClientIdOptimized(clientId, pageable);

        if (page.isEmpty()) {
            throw new NotFoundException("Nenhuma resposta encontrada para o Cliente com ID: " + clientId);
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

    public ChecklistResponseDetailDTO findChecklistResponseById(Long checklistResponseId) {
        ChecklistResponseEntity checklistResponse = findById(checklistResponseId);
        return convertToDetailDto(checklistResponse);
    }

    private ChecklistResponseDetailDTO convertToDetailDto(ChecklistResponseEntity checklistResponse) {
        ChecklistResponseDetailDTO dto = new ChecklistResponseDetailDTO();
        dto.setId(checklistResponse.getId());
        dto.setChecklistName(checklistResponse.getChecklistName());
        dto.setChecklistId(checklistResponse.getChecklistId());
        dto.setCreatedAt(checklistResponse.getCreatedAt());
        dto.setUserId(checklistResponse.getUser().getId());
        dto.setUserName(checklistResponse.getUser().getName());

        DamEntity dam = checklistResponse.getDam();
        DamInfoDTO damInfo = new DamInfoDTO();
        damInfo.setId(dam.getId());
        damInfo.setName(dam.getName());
        dto.setDam(damInfo);

        // ✅ Usar consulta otimizada com EntityGraph
        List<QuestionnaireResponseEntity> questionnaireResponses
                = questionnaireResponseRepository.findByChecklistResponseIdOptimized(checklistResponse.getId());

        // ✅ REMOVER ou simplificar drasticamente o fallback
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

        // ✅ Usar consulta otimizada com EntityGraph
        Page<ChecklistResponseEntity> page = checklistResponseRepository.findByDamIdOptimized(damId, pageable);
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
}
