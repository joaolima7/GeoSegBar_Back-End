package com.geosegbar.infra.checklist.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersAndDamDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersDTO;
import com.geosegbar.infra.checklist.dtos.OptionDTO;
import com.geosegbar.infra.checklist.dtos.QuestionWithLastAnswerDTO;
import com.geosegbar.infra.checklist.dtos.TemplateQuestionnaireWithAnswersDTO;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.dam.services.DamService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final ChecklistResponseRepository checklistResponseRepository;
    private final DamService damService;

    public List<ChecklistEntity> findAll() {
        return checklistRepository.findAll();
    }

    public ChecklistEntity findById(Long id) {
        return checklistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrada para id: " + id));
    }

    public ChecklistEntity save(ChecklistEntity checklist) {
        if (checklist.getDams() == null || checklist.getDams().isEmpty()) {
            throw new InvalidInputException("Checklist deve estar vinculado a uma barragem.");
        }
        if (checklist.getDams().size() > 1) {
            throw new InvalidInputException("Checklist só pode estar vinculado a uma única barragem.");
        }
        Long damId = checklist.getDams().iterator().next().getId();
        if (checklistRepository.existsByNameAndDams_Id(checklist.getName(), damId)) {
            throw new DuplicateResourceException("Já existe um checklist com esse nome para esta barragem.");
        }
        return checklistRepository.save(checklist);
    }

    public List<ChecklistWithLastAnswersDTO> findChecklistsWithLastAnswersForDam(Long damId) {
        // Verificar se a barragem existe
        damService.findById(damId);

        // Buscar todos os checklists associados à barragem
        List<ChecklistEntity> checklists = checklistRepository.findByDams_Id(damId);

        // Lista de DTOs a serem retornados
        List<ChecklistWithLastAnswersDTO> result = new ArrayList<>();

        // Para cada checklist associado à barragem
        for (ChecklistEntity checklist : checklists) {
            // Criar um DTO para este checklist
            ChecklistWithLastAnswersDTO checklistDTO = new ChecklistWithLastAnswersDTO();
            checklistDTO.setId(checklist.getId());
            checklistDTO.setName(checklist.getName());
            checklistDTO.setCreatedAt(checklist.getCreatedAt());

            // Buscar todas as respostas para este checklist específico nesta barragem, ordenadas por data (mais recentes primeiro)
            List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository
                    .findByDamIdAndChecklistIdOrderByCreatedAtDesc(damId, checklist.getId());

            // Lista de template questionnaires para este checklist
            List<TemplateQuestionnaireWithAnswersDTO> templateDTOs = new ArrayList<>();

            // Para cada template de questionário no checklist
            for (TemplateQuestionnaireEntity template : checklist.getTemplateQuestionnaires()) {
                TemplateQuestionnaireWithAnswersDTO templateDTO = new TemplateQuestionnaireWithAnswersDTO();
                templateDTO.setId(template.getId());
                templateDTO.setName(template.getName());

                // Lista de perguntas para este template
                List<QuestionWithLastAnswerDTO> questionDTOs = new ArrayList<>();

                // Para cada pergunta no template
                for (TemplateQuestionnaireQuestionEntity tqQuestion : template.getTemplateQuestions()) {
                    QuestionEntity question = tqQuestion.getQuestion();

                    // Criar um DTO para esta pergunta
                    QuestionWithLastAnswerDTO questionDTO = new QuestionWithLastAnswerDTO();
                    questionDTO.setId(question.getId());
                    questionDTO.setQuestionText(question.getQuestionText());
                    questionDTO.setType(question.getType());

                    // Converter todas as opções disponíveis para esta pergunta
                    List<OptionDTO> allOptionDTOs = question.getOptions().stream()
                            .map(opt -> new OptionDTO(opt.getId(), opt.getLabel(), opt.getValue()))
                            .collect(Collectors.toList());

                    questionDTO.setAllOptions(allOptionDTOs);

                    // Apenas buscar a última resposta não-NI se houver respostas para este checklist
                    if (!checklistResponses.isEmpty()) {
                        // Encontrar a última resposta não-NI para esta pergunta
                        Optional<OptionEntity> lastNonNIOption = findLastNonNIOption(checklistResponses, template.getId(), question.getId());

                        if (lastNonNIOption.isPresent()) {
                            OptionEntity option = lastNonNIOption.get();
                            OptionDTO optionDTO = new OptionDTO(option.getId(), option.getLabel(), option.getValue());
                            questionDTO.setLastSelectedOption(optionDTO);

                            // Encontrar o ID da resposta que contém esta opção
                            for (ChecklistResponseEntity response : checklistResponses) {
                                boolean found = false;
                                for (QuestionnaireResponseEntity qResponse : response.getQuestionnaireResponses()) {
                                    if (qResponse.getTemplateQuestionnaire().getId().equals(template.getId())) {
                                        for (AnswerEntity answer : qResponse.getAnswers()) {
                                            if (answer.getQuestion().getId().equals(question.getId())
                                                    && answer.getSelectedOptions().stream()
                                                            .anyMatch(opt -> opt.getId().equals(option.getId()))) {
                                                questionDTO.setAnswerResponseId(answer.getId());
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (found) {
                                        break;
                                    }
                                }
                                if (found) {
                                    break;
                                }
                            }
                        }
                    }
                    // Se não houver respostas ou nenhuma resposta não-NI, lastSelectedOption permanecerá null

                    // Adicionar esta pergunta à lista de perguntas para este template
                    questionDTOs.add(questionDTO);
                }

                // Definir a lista de perguntas para este template
                templateDTO.setQuestions(questionDTOs);

                // Adicionar este template à lista de templates para este checklist
                templateDTOs.add(templateDTO);
            }

            // Definir a lista de templates para este checklist
            checklistDTO.setTemplateQuestionnaires(templateDTOs);

            // Adicionar este checklist à lista de resultados
            result.add(checklistDTO);
        }

        return result;
    }

    public List<ChecklistWithLastAnswersAndDamDTO> findAllChecklistsWithLastAnswersByClientId(Long clientId) {
        // Buscar todas as barragens do cliente
        List<DamEntity> clientDams = damService.findDamsByClientId(clientId);

        List<ChecklistWithLastAnswersAndDamDTO> allChecklists = new ArrayList<>();

        // Para cada barragem do cliente
        for (DamEntity dam : clientDams) {
            // Buscar todos os checklists desta barragem
            List<ChecklistEntity> damChecklists = checklistRepository.findByDams_Id(dam.getId());

            // Para cada checklist da barragem
            for (ChecklistEntity checklist : damChecklists) {
                // Criar um DTO para este checklist
                ChecklistWithLastAnswersAndDamDTO checklistDTO = new ChecklistWithLastAnswersAndDamDTO();
                checklistDTO.setId(checklist.getId());
                checklistDTO.setName(checklist.getName());
                checklistDTO.setCreatedAt(checklist.getCreatedAt());

                // Adicionar informações da barragem
                ChecklistWithLastAnswersAndDamDTO.DamInfoDTO damInfo
                        = new ChecklistWithLastAnswersAndDamDTO.DamInfoDTO(
                                dam.getId(),
                                dam.getName(),
                                dam.getCity(),
                                dam.getState(),
                                dam.getLatitude(),
                                dam.getLongitude()
                        );
                checklistDTO.setDam(damInfo);

                // Buscar todas as respostas para este checklist específico nesta barragem
                List<ChecklistResponseEntity> checklistResponses = checklistResponseRepository
                        .findByDamIdAndChecklistIdOrderByCreatedAtDesc(dam.getId(), checklist.getId());

                // Lista de template questionnaires para este checklist
                List<TemplateQuestionnaireWithAnswersDTO> templateDTOs = new ArrayList<>();

                // Para cada template de questionário no checklist
                for (TemplateQuestionnaireEntity template : checklist.getTemplateQuestionnaires()) {
                    TemplateQuestionnaireWithAnswersDTO templateDTO = new TemplateQuestionnaireWithAnswersDTO();
                    templateDTO.setId(template.getId());
                    templateDTO.setName(template.getName());

                    // Lista de perguntas para este template
                    List<QuestionWithLastAnswerDTO> questionDTOs = new ArrayList<>();

                    // Para cada pergunta no template
                    for (TemplateQuestionnaireQuestionEntity tqQuestion : template.getTemplateQuestions()) {
                        QuestionEntity question = tqQuestion.getQuestion();

                        // Criar um DTO para esta pergunta
                        QuestionWithLastAnswerDTO questionDTO = new QuestionWithLastAnswerDTO();
                        questionDTO.setId(question.getId());
                        questionDTO.setQuestionText(question.getQuestionText());
                        questionDTO.setType(question.getType());

                        // Converter todas as opções disponíveis para esta pergunta
                        List<OptionDTO> allOptionDTOs = question.getOptions().stream()
                                .map(opt -> new OptionDTO(opt.getId(), opt.getLabel(), opt.getValue()))
                                .collect(Collectors.toList());

                        questionDTO.setAllOptions(allOptionDTOs);

                        // Apenas buscar a última resposta não-NI se houver respostas para este checklist
                        if (!checklistResponses.isEmpty()) {
                            // Encontrar a última resposta não-NI para esta pergunta
                            Optional<OptionEntity> lastNonNIOption = findLastNonNIOption(
                                    checklistResponses, template.getId(), question.getId());

                            if (lastNonNIOption.isPresent()) {
                                OptionEntity option = lastNonNIOption.get();
                                OptionDTO optionDTO = new OptionDTO(option.getId(), option.getLabel(), option.getValue());
                                questionDTO.setLastSelectedOption(optionDTO);

                                // Encontrar o ID da resposta que contém esta opção
                                for (ChecklistResponseEntity response : checklistResponses) {
                                    boolean found = false;
                                    for (QuestionnaireResponseEntity qResponse : response.getQuestionnaireResponses()) {
                                        if (qResponse.getTemplateQuestionnaire().getId().equals(template.getId())) {
                                            for (AnswerEntity answer : qResponse.getAnswers()) {
                                                if (answer.getQuestion().getId().equals(question.getId())
                                                        && answer.getSelectedOptions().stream()
                                                                .anyMatch(opt -> opt.getId().equals(option.getId()))) {
                                                    questionDTO.setAnswerResponseId(answer.getId());
                                                    found = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (found) {
                                            break;
                                        }
                                    }
                                    if (found) {
                                        break;
                                    }
                                }
                            }
                        }

                        questionDTOs.add(questionDTO);
                    }

                    templateDTO.setQuestions(questionDTOs);
                    templateDTOs.add(templateDTO);
                }

                checklistDTO.setTemplateQuestionnaires(templateDTOs);
                allChecklists.add(checklistDTO);
            }
        }

        // Ordenar por nome da barragem e depois por nome do checklist
        allChecklists.sort((a, b) -> {
            int damComparison = a.getDam().getName().compareTo(b.getDam().getName());
            return damComparison != 0 ? damComparison : a.getName().compareTo(b.getName());
        });

        return allChecklists;
    }

    private Optional<OptionEntity> findLastNonNIOption(
            List<ChecklistResponseEntity> checklistResponses,
            Long templateId,
            Long questionId) {

        for (ChecklistResponseEntity response : checklistResponses) {
            for (QuestionnaireResponseEntity qResponse : response.getQuestionnaireResponses()) {
                if (qResponse.getTemplateQuestionnaire().getId().equals(templateId)) {
                    for (AnswerEntity answer : qResponse.getAnswers()) {
                        if (answer.getQuestion().getId().equals(questionId)) {
                            // Procurar por uma opção não-NI nesta resposta
                            Optional<OptionEntity> nonNIOption = answer.getSelectedOptions().stream()
                                    .filter(opt -> !"NI".equalsIgnoreCase(opt.getLabel()))
                                    .findFirst();

                            if (nonNIOption.isPresent()) {
                                return nonNIOption;
                            }
                        }
                    }
                }
            }
        }

        // Se não encontrou nenhuma opção não-NI
        return Optional.empty();
    }

    public ChecklistEntity update(ChecklistEntity checklist) {
        if (checklist.getDams() == null || checklist.getDams().isEmpty()) {
            throw new InvalidInputException("Checklist deve estar vinculado a uma barragem.");
        }
        if (checklist.getDams().size() > 1) {
            throw new InvalidInputException("Checklist só pode estar vinculado a uma única barragem.");
        }
        Long damId = checklist.getDams().iterator().next().getId();
        if (checklistRepository.existsByNameAndDams_IdAndIdNot(checklist.getName(), damId, checklist.getId())) {
            throw new DuplicateResourceException("Já existe um checklist com esse nome para esta barragem.");
        }
        return checklistRepository.save(checklist);
    }

    @Transactional
    public void deleteById(Long id) {
        checklistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrada para exclusão!"));
        checklistRepository.deleteById(id);
    }

    public List<ChecklistEntity> findByDamId(Long damId) {
        return checklistRepository.findByDams_Id(damId);
    }

    public ChecklistEntity findChecklistForDam(Long damId, Long checklistId) {
        ChecklistEntity checklist = findById(checklistId);

        if (checklist.getDams().stream().anyMatch(dam -> dam.getId().equals(damId))) {
            return checklist;
        } else {
            throw new NotFoundException("O checklist não pertence à barragem especificada!");
        }
    }

}
