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
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist_response.dtos.ChecklistResponseDetailDTO;
import com.geosegbar.infra.checklist_response.dtos.ClientDetailedChecklistResponsesDTO;
import com.geosegbar.infra.checklist_response.dtos.DamInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.DamLastChecklistDTO;
import com.geosegbar.infra.checklist_response.dtos.OptionInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.PagedChecklistResponseDTO;
import com.geosegbar.infra.checklist_response.dtos.PhotoInfoDTO;
import com.geosegbar.infra.checklist_response.dtos.QuestionWithAnswerDTO;
import com.geosegbar.infra.checklist_response.dtos.TemplateWithAnswersDTO;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
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
    private final ClientRepository clientRepository;

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
        return responses;
    }

    @Transactional

    public ChecklistResponseEntity save(ChecklistResponseEntity checklistResponse) {
        Long damId = checklistResponse.getDam().getId();
        DamEntity dam = damService.findById(damId);
        checklistResponse.setDam(dam);

        ChecklistResponseEntity saved = checklistResponseRepository.save(checklistResponse);

        return saved;
    }

    @Transactional

    public ChecklistResponseEntity update(ChecklistResponseEntity checklistResponse) {
        ChecklistResponseEntity existing = checklistResponseRepository.findById(checklistResponse.getId())
                .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para atualização!"));

        Long oldDamId = existing.getDam().getId();
        DamEntity oldDam = damService.findById(oldDamId);
        Long oldClientId = oldDam.getClient().getId();
        Long oldUserId = existing.getUser().getId();

        Long newDamId = checklistResponse.getDam().getId();
        DamEntity newDam = damService.findById(newDamId);
        checklistResponse.setDam(newDam);
        Long newClientId = newDam.getClient().getId();
        Long newUserId = checklistResponse.getUser().getId();

        ChecklistResponseEntity saved = checklistResponseRepository.save(checklistResponse);

        return saved;
    }

    @Transactional

    public void deleteById(Long id) {
        ChecklistResponseEntity existing = checklistResponseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resposta de Checklist não encontrada para exclusão!"));

        Long damId = existing.getDam().getId();
        DamEntity dam = damService.findById(damId);
        Long clientId = dam.getClient().getId();
        Long userId = existing.getUser().getId();

        checklistResponseRepository.deleteById(id);
    }

    public List<ChecklistResponseDetailDTO> findChecklistResponsesByDamId(Long damId) {
        damService.findById(damId);

        List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository.findByDamId(damId);

        return checklistResponses.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findChecklistResponsesByClientIdPaged(
            Long clientId, Pageable pageable) {

        Page<ChecklistResponseEntity> page = checklistResponseRepository.findByClientIdOptimized(clientId, pageable);

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

        List<TemplateWithAnswersDTO> templates = new ArrayList<>(templateMap.values());
        dto.setTemplates(templates);

        return dto;
    }

    public List<ChecklistResponseDetailDTO> findChecklistResponsesByUserId(Long userId) {
        List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository.findByUserId(userId);

        return checklistResponses.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    public List<ChecklistResponseDetailDTO> findChecklistResponsesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository.findByCreatedAtBetween(startDate, endDate);

        return checklistResponses.stream()
                .map(this::convertToDetailDto)
                .collect(Collectors.toList());
    }

    public PagedChecklistResponseDTO<ChecklistResponseDetailDTO> findChecklistResponsesByDamIdPaged(Long damId, Pageable pageable) {
        damService.findById(damId);

        Page<ChecklistResponseEntity> page = checklistResponseRepository.findByDamIdOptimized(damId, pageable);

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

    public ClientDetailedChecklistResponsesDTO findLatestDetailedChecklistResponsesByClientId(Long clientId, int limit) {

        ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com ID: " + clientId));

        List<Long> responseIds = checklistResponseRepository.findLatestChecklistResponseIdsByClientIdAndLimit(clientId, limit);

        if (responseIds.isEmpty()) {
            return new ClientDetailedChecklistResponsesDTO(clientId, client.getName(), List.of());
        }

        List<ChecklistResponseEntity> responses = new ArrayList<>();
        for (Long id : responseIds) {

            ChecklistResponseEntity response = checklistResponseRepository.findByIdWithFullDetails(id)
                    .orElse(null);
            if (response != null) {
                responses.add(response);
            }
        }

        Map<Long, ClientDetailedChecklistResponsesDTO.ChecklistWithDetailedResponsesDTO> checklistMap = new HashMap<>();

        for (ChecklistResponseEntity response : responses) {
            Long checklistId = response.getChecklistId();

            ClientDetailedChecklistResponsesDTO.ChecklistWithDetailedResponsesDTO checklistDto
                    = checklistMap.computeIfAbsent(
                            checklistId,
                            id -> new ClientDetailedChecklistResponsesDTO.ChecklistWithDetailedResponsesDTO(
                                    checklistId,
                                    response.getChecklistName(),
                                    new ArrayList<>()
                            )
                    );

            ChecklistResponseDetailDTO detailedDto = convertToDetailDto(response);
            checklistDto.getLatestResponses().add(detailedDto);
        }

        return new ClientDetailedChecklistResponsesDTO(
                clientId,
                client.getName(),
                new ArrayList<>(checklistMap.values())
        );
    }
}
