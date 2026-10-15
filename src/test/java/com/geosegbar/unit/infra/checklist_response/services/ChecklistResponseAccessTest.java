package com.geosegbar.unit.infra.checklist_response.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.RoutineInspectionPermissionEntity;
import com.geosegbar.entities.TemplateQuestionnaireEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.checklist_response.dtos.ChecklistResponseEditContextDTO;
import com.geosegbar.infra.questionnaire_response.persistence.jpa.QuestionnaireResponseRepository;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.checklist_response.dtos.ClientDetailedChecklistResponsesDTO;
import com.geosegbar.infra.checklist_response.dtos.PagedChecklistResponseDTO;
import com.geosegbar.infra.checklist_response.persistence.jpa.ChecklistResponseRepository;
import com.geosegbar.infra.checklist_response.services.ChecklistResponseService;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.dam.services.DamAccessService;
import com.geosegbar.infra.dam.services.DamService;

/**
 * As listagens de respostas respeitam o acesso do usuário à barragem (V5-B01).
 *
 * O formato das respostas não muda — o app da main continua lendo o mesmo
 * corpo —, só o que entra nele.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("ChecklistResponseService — acesso à barragem nas listagens")
class ChecklistResponseAccessTest extends BaseUnitTest {

    @Mock
    private ChecklistResponseRepository checklistResponseRepository;
    @Mock
    private DamService damService;
    @Mock
    private DamAccessService damAccessService;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private AnomalyRepository anomalyRepository;
    @Mock
    private QuestionnaireResponseRepository questionnaireResponseRepository;
    @Mock
    private AnswerRepository answerRepository;

    @InjectMocks
    private ChecklistResponseService service;

    private final Pageable pageable = PageRequest.of(0, 5);
    private ClientEntity client;

    @BeforeEach
    void setUp() {
        client = new ClientEntity();
        client.setId(1L);
        client.setName("Cliente");
    }

    @Test
    @DisplayName("histórico do cliente filtra pelas barragens acessíveis")
    void clientHistoryIsRestrictedToAccessibleDams() {
        when(damAccessService.listingRestriction()).thenReturn(Optional.of(Set.of(10L)));
        when(checklistResponseRepository.findByClientIdAndDamIdsOptimized(eq(1L), eq(Set.of(10L)), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        PagedChecklistResponseDTO<?> result = service.findChecklistResponsesByClientIdPaged(1L, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(checklistResponseRepository, never()).findByClientIdOptimized(anyLong(), any());
    }

    @Test
    @DisplayName("ADMIN continua vendo o cliente inteiro pela consulta antiga")
    void adminKeepsUnrestrictedQuery() {
        when(damAccessService.listingRestriction()).thenReturn(Optional.empty());
        when(checklistResponseRepository.findByClientIdOptimized(1L, pageable)).thenReturn(Page.empty(pageable));

        service.findChecklistResponsesByClientIdPaged(1L, pageable);

        verify(checklistResponseRepository).findByClientIdOptimized(1L, pageable);
        verify(checklistResponseRepository, never()).findByClientIdAndDamIdsOptimized(anyLong(), any(), any());
    }

    @Test
    @DisplayName("sem nenhuma barragem acessível, página vazia sem consultar o banco")
    void noAccessibleDamReturnsEmptyPageWithoutQuery() {
        when(damAccessService.listingRestriction()).thenReturn(Optional.of(Set.of()));

        PagedChecklistResponseDTO<?> result = service.findChecklistResponsesByClientIdPaged(1L, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(checklistResponseRepository, never()).findByClientIdAndDamIdsOptimized(anyLong(), any(), any());
        verify(checklistResponseRepository, never()).findByClientIdOptimized(anyLong(), any());
    }

    @Test
    @DisplayName("últimas respostas por barragem passam restrição e partição para a consulta")
    void latestResponsesPerDamAreRestricted() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(damAccessService.listingRestriction()).thenReturn(Optional.of(Set.of(10L)));
        when(checklistResponseRepository.findLatestChecklistResponseIdsByClientId(1L, true, Set.of(10L), true, 3))
                .thenReturn(List.of());

        ClientDetailedChecklistResponsesDTO result = service.findLatestDetailedChecklistResponsesByClientId(1L, 3, true);

        assertThat(result.getChecklists()).isEmpty();
        verify(checklistResponseRepository).findLatestChecklistResponseIdsByClientId(1L, true, Set.of(10L), true, 3);
    }

    @Test
    @DisplayName("sem perDam a partição continua a do app da main (por checklist)")
    void latestResponsesDefaultKeepsPerChecklistPartition() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(damAccessService.listingRestriction()).thenReturn(Optional.empty());
        when(checklistResponseRepository.findLatestChecklistResponseIdsByClientId(eq(1L), eq(false), any(), eq(false), eq(3)))
                .thenReturn(List.of());

        service.findLatestDetailedChecklistResponsesByClientId(1L, 3);

        verify(checklistResponseRepository).findLatestChecklistResponseIdsByClientId(eq(1L), eq(false), any(), eq(false), eq(3));
    }

    @Test
    @DisplayName("últimas respostas sem barragem acessível não consultam o banco")
    void latestResponsesWithoutAccessReturnEmpty() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(damAccessService.listingRestriction()).thenReturn(Optional.of(Set.of()));

        ClientDetailedChecklistResponsesDTO result = service.findLatestDetailedChecklistResponsesByClientId(1L, 3, true);

        assertThat(result.getChecklists()).isEmpty();
        verify(checklistResponseRepository, never())
                .findLatestChecklistResponseIdsByClientId(anyLong(), anyBoolean(), any(), anyBoolean(), anyInt());
    }

    @Test
    @DisplayName("histórico de uma barragem sem acesso é 403 e não consulta")
    void damHistoryWithoutAccessIsForbidden() {
        doThrow(new ForbiddenException("sem acesso")).when(damAccessService).requireAccess(20L);

        assertThatThrownBy(() -> service.findChecklistResponsesByDamIdPaged(20L, pageable))
                .isInstanceOf(ForbiddenException.class);
        verify(checklistResponseRepository, never()).findByDamIdOptimized(anyLong(), any());
    }

    @Test
    @DisplayName("detalhe de uma resposta confere o acesso à barragem dela")
    void responseDetailChecksDamAccess() {
        DamEntity dam = new DamEntity();
        dam.setId(20L);
        dam.setClient(client);
        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setId(7L);
        response.setDam(dam);
        response.setCreatedAt(LocalDateTime.now());
        when(checklistResponseRepository.findByIdWithBasicInfo(7L)).thenReturn(Optional.of(response));
        doThrow(new ForbiddenException("sem acesso")).when(damAccessService).requireAccess(20L);

        assertThatThrownBy(() -> service.findChecklistResponseById(7L))
                .isInstanceOf(ForbiddenException.class);
    }

    // ==================== V5-B03: contexto de edição ====================

    private ChecklistResponseEntity responseWithOneAnswer(LocalDateTime createdAt) {
        DamEntity dam = new DamEntity();
        dam.setId(10L);
        dam.setClient(client);

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setId(7L);
        response.setDam(dam);
        response.setCreatedAt(createdAt);

        TemplateQuestionnaireEntity template = new TemplateQuestionnaireEntity();
        template.setId(70L);
        template.setName("Talude");
        QuestionEntity question = new QuestionEntity();
        question.setId(31L);
        question.setQuestionText("Há trincas?");
        AnswerEntity answer = new AnswerEntity();
        answer.setId(500L);
        answer.setQuestion(question);
        QuestionnaireResponseEntity questionnaire = new QuestionnaireResponseEntity();
        questionnaire.setId(90L);
        questionnaire.setTemplateQuestionnaire(template);
        questionnaire.getAnswers().add(answer);

        when(checklistResponseRepository.findByIdWithBasicInfo(7L)).thenReturn(Optional.of(response));
        when(questionnaireResponseRepository.findByChecklistResponseIdOptimized(7L))
                .thenReturn(new ArrayList<>(List.of(questionnaire)));
        return response;
    }

    private MockedStatic<AuthenticatedUserUtil> userWith(boolean fillWeb, boolean fillMobile) {
        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setIsFillWeb(fillWeb);
        permission.setIsFillMobile(fillMobile);
        UserEntity user = new UserEntity();
        user.setId(3L);
        user.setRoutineInspectionPermission(permission);

        MockedStatic<AuthenticatedUserUtil> auth = Mockito.mockStatic(AuthenticatedUserUtil.class);
        auth.when(AuthenticatedUserUtil::isAdmin).thenReturn(false);
        auth.when(AuthenticatedUserUtil::getCurrentUser).thenReturn(user);
        return auth;
    }

    @Test
    @DisplayName("contexto de edição traz a opção anterior por (questionário, pergunta), na data da inspeção")
    void editContextBringsPreviousLabelsAtInspectionDate() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 10, 9, 30);
        responseWithOneAnswer(createdAt);
        when(answerRepository.findRelevantOptionLabelsBefore(List.of(31L), List.of(70L), 10L, 7L, createdAt))
                .thenReturn(List.<Object[]>of(new Object[]{31L, 70L, "PV"}));

        try (MockedStatic<AuthenticatedUserUtil> auth = userWith(false, true)) {
            ChecklistResponseEditContextDTO context = service.findEditContext(7L, null);

            assertThat(context.referenceDate()).isEqualTo(createdAt);
            assertThat(context.response().getTemplates()).hasSize(1);
            assertThat(context.response().getTemplates().get(0).getQuestionsWithAnswers().get(0).getAnswerId())
                    .isEqualTo(500L);
            assertThat(context.previousLabels()).containsExactly(
                    new ChecklistResponseEditContextDTO.PreviousLabel(70L, 31L, "PV"));
        }
    }

    @Test
    @DisplayName("contexto de edição com outra data recalcula a referência")
    void editContextUsesRequestedDate() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 10, 9, 30);
        LocalDateTime earlier = LocalDateTime.of(2026, 9, 30, 12, 0);
        responseWithOneAnswer(createdAt);
        when(answerRepository.findRelevantOptionLabelsBefore(List.of(31L), List.of(70L), 10L, 7L, earlier))
                .thenReturn(List.of());

        try (MockedStatic<AuthenticatedUserUtil> auth = userWith(true, false)) {
            ChecklistResponseEditContextDTO context = service.findEditContext(7L, earlier);

            assertThat(context.referenceDate()).isEqualTo(earlier);
            assertThat(context.previousLabels()).isEmpty();
        }
    }

    @Test
    @DisplayName("contexto de edição recusa data no futuro")
    void editContextRejectsFutureDate() {
        when(checklistResponseRepository.findByIdWithBasicInfo(7L)).thenReturn(Optional.of(responseStub()));

        try (MockedStatic<AuthenticatedUserUtil> auth = userWith(true, true)) {
            assertThatThrownBy(() -> service.findEditContext(7L, LocalDateTime.now().plusDays(1)))
                    .isInstanceOf(InvalidInputException.class);
        }
    }

    @Test
    @DisplayName("sem permissão de preenchimento, editar inspeção é 403")
    void editWithoutFillPermissionIsForbidden() {
        when(checklistResponseRepository.findByIdWithBasicInfo(7L)).thenReturn(Optional.of(responseStub()));

        try (MockedStatic<AuthenticatedUserUtil> auth = userWith(false, false)) {
            assertThatThrownBy(() -> service.findEditContext(7L, null))
                    .isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> service.updateChecklistResponse(7L,
                    new com.geosegbar.infra.checklist_response.dtos.ChecklistResponseUpdateDTO()))
                    .isInstanceOf(ForbiddenException.class);
        }
        verify(checklistResponseRepository, never()).updateTopLevelFields(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("sem acesso à barragem, editar inspeção é 403")
    void editWithoutDamAccessIsForbidden() {
        when(checklistResponseRepository.findByIdWithBasicInfo(7L)).thenReturn(Optional.of(responseStub()));
        doThrow(new ForbiddenException("sem acesso")).when(damAccessService).requireAccess(10L);

        assertThatThrownBy(() -> service.findEditContext(7L, null))
                .isInstanceOf(ForbiddenException.class);
    }

    private ChecklistResponseEntity responseStub() {
        DamEntity dam = new DamEntity();
        dam.setId(10L);
        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setId(7L);
        response.setDam(dam);
        response.setCreatedAt(LocalDateTime.of(2026, 10, 10, 9, 30));
        return response;
    }
}
