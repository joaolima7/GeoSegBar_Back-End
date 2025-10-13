package com.geosegbar.infra.checklist.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.TemplateQuestionnaireQuestionEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersAndDamDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistWithLastAnswersDTO;
import com.geosegbar.infra.checklist.dtos.OptionDTO;
import com.geosegbar.infra.checklist.dtos.QuestionWithLastAnswerDTO;
import com.geosegbar.infra.checklist.dtos.TemplateQuestionnaireWithAnswersDTO;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.dam.services.DamService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final DamService damService;
    private final AnswerRepository answerRepository;

    public Page<ChecklistEntity> findAllPaged(Pageable pageable) {
        return checklistRepository.findAllWithDams(pageable);
    }

    @Cacheable(value = "checklistById", key = "#id", cacheManager = "checklistCacheManager")
    public ChecklistEntity findById(Long id) {
        return checklistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrada para id: " + id));
    }

    @Transactional
    @CacheEvict(value = {"allChecklists", "checklistById", "checklistsByDam",
        "checklistsWithAnswersByDam", "checklistsWithAnswersByClient", "checklistForDam"},
            allEntries = true, cacheManager = "checklistCacheManager")
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

    @Transactional()
    @Cacheable(value = "checklistsWithAnswersByDam", key = "#damId", cacheManager = "checklistCacheManager")
    public List<ChecklistWithLastAnswersDTO> findChecklistsWithLastAnswersForDam(Long damId) {
        // Verificar se a barragem existe
        damService.findById(damId);

        // Buscar todos os checklists associados à barragem - agora com todos os detalhes
        List<ChecklistEntity> checklists = checklistRepository.findByDamIdWithFullDetails(damId);

        // Lista para guardar os resultados
        List<ChecklistWithLastAnswersDTO> result = new ArrayList<>();

        // Para cada checklist, obter a última resposta com uma única query
        for (ChecklistEntity checklist : checklists) {
            ChecklistWithLastAnswersDTO checklistDTO = new ChecklistWithLastAnswersDTO();
            checklistDTO.setId(checklist.getId());
            checklistDTO.setName(checklist.getName());
            checklistDTO.setCreatedAt(checklist.getCreatedAt());

            List<TemplateQuestionnaireWithAnswersDTO> templateDTOs = new ArrayList<>();

            // Para cada template, buscar a última resposta não-NI para cada pergunta em uma única query
            for (TemplateQuestionnaireEntity template : checklist.getTemplateQuestionnaires()) {
                TemplateQuestionnaireWithAnswersDTO templateDTO = new TemplateQuestionnaireWithAnswersDTO();
                templateDTO.setId(template.getId());
                templateDTO.setName(template.getName());

                List<QuestionWithLastAnswerDTO> questionDTOs = new ArrayList<>();

                for (TemplateQuestionnaireQuestionEntity tqQuestion : template.getTemplateQuestions()) {
                    QuestionEntity question = tqQuestion.getQuestion();

                    QuestionWithLastAnswerDTO questionDTO = new QuestionWithLastAnswerDTO();
                    questionDTO.setId(question.getId());
                    questionDTO.setQuestionText(question.getQuestionText());
                    questionDTO.setType(question.getType());

                    // Converter todas as opções disponíveis
                    List<OptionDTO> allOptionDTOs = question.getOptions().stream()
                            .map(opt -> new OptionDTO(opt.getId(), opt.getLabel(), opt.getValue()))
                            .collect(Collectors.toList());
                    questionDTO.setAllOptions(allOptionDTOs);

                    // Buscar a última resposta não-NI com uma única query otimizada
                    Optional<AnswerEntity> lastNonNIAnswer = answerRepository.findLatestNonNIAnswer(
                            damId, question.getId(), template.getId());

                    lastNonNIAnswer.ifPresent(answer -> {
                        // Selecionar a primeira opção não-NI
                        Optional<OptionEntity> nonNIOption = answer.getSelectedOptions().stream()
                                .filter(opt -> !"NI".equalsIgnoreCase(opt.getLabel()))
                                .findFirst();

                        nonNIOption.ifPresent(option -> {
                            OptionDTO optionDTO = new OptionDTO(
                                    option.getId(), option.getLabel(), option.getValue());
                            questionDTO.setLastSelectedOption(optionDTO);
                            questionDTO.setAnswerResponseId(answer.getId());
                        });
                    });

                    questionDTOs.add(questionDTO);
                }

                templateDTO.setQuestions(questionDTOs);
                templateDTOs.add(templateDTO);
            }

            checklistDTO.setTemplateQuestionnaires(templateDTOs);
            result.add(checklistDTO);
        }

        return result;
    }

    @Transactional()
    @Cacheable(value = "checklistsWithAnswersByClient", key = "#clientId", cacheManager = "checklistCacheManager")
    public List<ChecklistWithLastAnswersAndDamDTO> findAllChecklistsWithLastAnswersByClientId(Long clientId) {
        // Buscar todas as barragens do cliente
        List<DamEntity> clientDams = damService.findDamsByClientId(clientId);
        List<ChecklistWithLastAnswersAndDamDTO> allChecklists = new ArrayList<>();

        // Para cada barragem do cliente
        for (DamEntity dam : clientDams) {
            // Buscar todos os checklists desta barragem com detalhes completos
            List<ChecklistEntity> damChecklists = checklistRepository.findByDamIdWithFullDetails(dam.getId());

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

                        // Buscar a última resposta não-NI com uma única query otimizada
                        Optional<AnswerEntity> lastNonNIAnswer = answerRepository.findLatestNonNIAnswer(
                                dam.getId(), question.getId(), template.getId());

                        lastNonNIAnswer.ifPresent(answer -> {
                            // Selecionar a primeira opção não-NI
                            Optional<OptionEntity> nonNIOption = answer.getSelectedOptions().stream()
                                    .filter(opt -> !"NI".equalsIgnoreCase(opt.getLabel()))
                                    .findFirst();

                            nonNIOption.ifPresent(option -> {
                                OptionDTO optionDTO = new OptionDTO(
                                        option.getId(), option.getLabel(), option.getValue());
                                questionDTO.setLastSelectedOption(optionDTO);
                                questionDTO.setAnswerResponseId(answer.getId());
                            });
                        });

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

    @Transactional
    @CacheEvict(value = {"allChecklists", "checklistById", "checklistsByDam",
        "checklistsWithAnswersByDam", "checklistsWithAnswersByClient", "checklistForDam"},
            allEntries = true, cacheManager = "checklistCacheManager")
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
    @CacheEvict(value = {"allChecklists", "checklistById", "checklistsByDam",
        "checklistsWithAnswersByDam", "checklistsWithAnswersByClient", "checklistForDam"},
            allEntries = true, cacheManager = "checklistCacheManager")
    public void deleteById(Long id) {
        checklistRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Checklist não encontrada para exclusão!"));
        checklistRepository.deleteById(id);
    }

    @Cacheable(value = "checklistsByDam", key = "#damId", cacheManager = "checklistCacheManager")
    public List<ChecklistEntity> findByDamId(Long damId) {
        return checklistRepository.findByDams_Id(damId);
    }

    @Cacheable(value = "checklistForDam", key = "#damId + '_' + #checklistId", cacheManager = "checklistCacheManager")
    public ChecklistEntity findChecklistForDam(Long damId, Long checklistId) {
        ChecklistEntity checklist = findById(checklistId);

        if (checklist.getDams().stream().anyMatch(dam -> dam.getId().equals(damId))) {
            return checklist;
        } else {
            throw new NotFoundException("O checklist não pertence à barragem especificada!");
        }
    }

}
