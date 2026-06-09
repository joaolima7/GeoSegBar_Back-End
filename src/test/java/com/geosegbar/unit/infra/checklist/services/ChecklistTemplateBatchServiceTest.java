package com.geosegbar.unit.infra.checklist.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ChecklistEntity;
import com.geosegbar.entities.ChecklistTemplateEntity;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.checklist.dtos.ChecklistTemplateAssociationsRequestDTO;
import com.geosegbar.infra.checklist.dtos.ChecklistTemplateAssociationsResponseDTO;
import com.geosegbar.infra.checklist.dtos.TemplateOrderDTO;
import com.geosegbar.infra.checklist.persistence.jpa.ChecklistRepository;
import com.geosegbar.infra.checklist.services.ChecklistService;
import com.geosegbar.infra.checklist_template.persistence.jpa.ChecklistTemplateRepository;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;
import com.geosegbar.infra.question.persistence.jpa.QuestionRepository;
import com.geosegbar.infra.question.services.QuestionService;
import com.geosegbar.infra.template_questionnaire.persistence.jpa.TemplateQuestionnaireRepository;

@Tag("unit")
@DisplayName("Checklist template batch service tests")
class ChecklistTemplateBatchServiceTest extends BaseUnitTest {

    @Mock
    private ChecklistRepository checklistRepository;

    @Mock
    private ChecklistTemplateRepository checklistTemplateRepository;

    @Mock
    private DamService damService;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private TemplateQuestionnaireRepository templateQuestionnaireRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private OptionRepository optionRepository;

    @InjectMocks
    private ChecklistService checklistService;

    private ChecklistEntity checklist;
    private DamEntity dam;
    private TemplateQuestionnaireEntity templateOne;
    private TemplateQuestionnaireEntity templateTwo;
    private TemplateQuestionnaireEntity templateThree;
    private ChecklistTemplateEntity associationOne;
    private ChecklistTemplateEntity associationTwo;

    @BeforeEach
    void setUp() {
        ClientEntity client = new ClientEntity();
        client.setId(1L);

        dam = new DamEntity();
        dam.setId(10L);
        dam.setClient(client);

        checklist = new ChecklistEntity();
        checklist.setId(100L);
        checklist.setDam(dam);

        templateOne = template(1L);
        templateTwo = template(2L);
        templateThree = template(3L);

        associationOne = association(101L, templateOne, 1);
        associationTwo = association(102L, templateTwo, 2);
    }

    @Test
    @DisplayName("Should associate and disassociate templates in one request and persist final order")
    void shouldUpdateTemplateAssociationsInBatch() {
        ChecklistTemplateAssociationsRequestDTO request = new ChecklistTemplateAssociationsRequestDTO(
                List.of(3L),
                List.of(2L),
                List.of(
                        new TemplateOrderDTO(1L, 1),
                        new TemplateOrderDTO(3L, 2)
                )
        );

        when(checklistRepository.findByIdWithTemplates(100L)).thenReturn(Optional.of(checklist));
        when(checklistTemplateRepository.findByChecklistIdOrderByOrderIndex(100L))
                .thenReturn(List.of(associationOne, associationTwo));
        when(templateQuestionnaireRepository.findAllById(any())).thenReturn(List.of(templateThree));
        when(checklistTemplateRepository.save(any(ChecklistTemplateEntity.class))).thenAnswer(invocation -> {
            ChecklistTemplateEntity saved = invocation.getArgument(0);
            saved.setId(103L);
            return saved;
        });
        when(checklistTemplateRepository.saveAll(any())).thenAnswer(invocation -> toList(invocation.getArgument(0)));

        ChecklistTemplateAssociationsResponseDTO response = checklistService.updateTemplateAssociations(100L, request);

        assertThat(response.getAssociatedTemplateIds()).containsExactly(3L);
        assertThat(response.getDisassociatedTemplateIds()).containsExactly(2L);
        assertThat(response.getTemplateCount()).isEqualTo(2);
        assertThat(response.getItems())
                .extracting("templateId", "orderIndex")
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(1L, 1),
                        org.assertj.core.api.Assertions.tuple(3L, 2)
                );

        verify(checklistTemplateRepository).deleteByIdNative(102L);
        verify(checklistTemplateRepository).flush();
    }

    private TemplateQuestionnaireEntity template(Long id) {
        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setId(id);
        template.setDam(dam);
        return template;
    }

    private ChecklistTemplateEntity association(Long id, TemplateQuestionnaireEntity template, Integer orderIndex) {
        ChecklistTemplateEntity association = new ChecklistTemplateEntity();
        association.setId(id);
        association.setChecklist(checklist);
        association.setTemplateQuestionnaire(template);
        association.setOrderIndex(orderIndex);
        return association;
    }

    private List<ChecklistTemplateEntity> toList(Iterable<ChecklistTemplateEntity> iterable) {
        List<ChecklistTemplateEntity> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}
