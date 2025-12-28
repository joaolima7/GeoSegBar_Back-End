package com.geosegbar.unit.entities;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ChecklistResponseEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - ChecklistResponseEntity")
class ChecklistResponseEntityTest extends BaseUnitTest {

    private DamEntity dam;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        dam = TestDataBuilder.dam().withName("Barragem Teste").build();
        user = TestDataBuilder.user().asCollaborator().build();
    }

    @Test
    @DisplayName("Should create checklist response with required fields")
    void shouldCreateChecklistResponseWithRequiredFields() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setId(1L);
        response.setChecklistName("Checklist de Segurança");
        response.setChecklistId(100L);
        response.setDam(dam);
        response.setUser(user);

        assertThat(response).satisfies(r -> {
            assertThat(r.getId()).isEqualTo(1L);
            assertThat(r.getChecklistName()).isEqualTo("Checklist de Segurança");
            assertThat(r.getChecklistId()).isEqualTo(100L);
            assertThat(r.getDam()).isEqualTo(dam);
            assertThat(r.getUser()).isEqualTo(user);
            assertThat(r.getQuestionnaireResponses()).isNotNull().isEmpty();
        });
    }

    @Test
    @DisplayName("Should create checklist response using all args constructor")
    void shouldCreateChecklistResponseUsingAllArgsConstructor() {

        LocalDateTime now = LocalDateTime.now();
        HashSet<QuestionnaireResponseEntity> responses = new HashSet<>();

        ChecklistResponseEntity checklistResponse = new ChecklistResponseEntity(
                1L,
                "Checklist Mensal",
                100L,
                now,
                dam,
                user,
                responses
        );

        assertThat(checklistResponse).satisfies(r -> {
            assertThat(r.getId()).isEqualTo(1L);
            assertThat(r.getChecklistName()).isEqualTo("Checklist Mensal");
            assertThat(r.getChecklistId()).isEqualTo(100L);
            assertThat(r.getCreatedAt()).isEqualTo(now);
            assertThat(r.getDam()).isEqualTo(dam);
            assertThat(r.getUser()).isEqualTo(user);
            assertThat(r.getQuestionnaireResponses()).isEqualTo(responses);
        });
    }

    @Test
    @DisplayName("Should create checklist response with no args constructor")
    void shouldCreateChecklistResponseWithNoArgsConstructor() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getChecklistName()).isNull();
        assertThat(response.getChecklistId()).isNull();
        assertThat(response.getCreatedAt()).isNull();
        assertThat(response.getDam()).isNull();
        assertThat(response.getUser()).isNull();
        assertThat(response.getQuestionnaireResponses()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should initialize questionnaire responses as empty HashSet")
    void shouldInitializeQuestionnaireResponsesAsEmptyHashSet() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();

        assertThat(response.getQuestionnaireResponses())
                .isNotNull()
                .isInstanceOf(HashSet.class)
                .isEmpty();
    }

    @Test
    @DisplayName("Should add questionnaire response")
    void shouldAddQuestionnaireResponse() {

        ChecklistResponseEntity checklistResponse = new ChecklistResponseEntity();
        checklistResponse.setId(1L);

        QuestionnaireResponseEntity questionnaireResponse = new QuestionnaireResponseEntity();
        questionnaireResponse.setId(1L);
        questionnaireResponse.setChecklistResponse(checklistResponse);

        checklistResponse.getQuestionnaireResponses().add(questionnaireResponse);

        assertThat(checklistResponse.getQuestionnaireResponses())
                .hasSize(1)
                .contains(questionnaireResponse);
    }

    @Test
    @DisplayName("Should add multiple questionnaire responses")
    void shouldAddMultipleQuestionnaireResponses() {

        ChecklistResponseEntity checklistResponse = new ChecklistResponseEntity();
        checklistResponse.setId(1L);

        QuestionnaireResponseEntity response1 = new QuestionnaireResponseEntity();
        response1.setId(1L);
        response1.setChecklistResponse(checklistResponse);

        QuestionnaireResponseEntity response2 = new QuestionnaireResponseEntity();
        response2.setId(2L);
        response2.setChecklistResponse(checklistResponse);

        QuestionnaireResponseEntity response3 = new QuestionnaireResponseEntity();
        response3.setId(3L);
        response3.setChecklistResponse(checklistResponse);

        checklistResponse.getQuestionnaireResponses().add(response1);
        checklistResponse.getQuestionnaireResponses().add(response2);
        checklistResponse.getQuestionnaireResponses().add(response3);

        assertThat(checklistResponse.getQuestionnaireResponses())
                .hasSize(3)
                .containsExactlyInAnyOrder(response1, response2, response3);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with dam")
    void shouldMaintainManyToOneRelationshipWithDam() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setDam(dam);

        assertThat(response.getDam())
                .isNotNull()
                .isEqualTo(dam);
        assertThat(response.getDam().getName()).isEqualTo("Barragem Teste");
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with user")
    void shouldMaintainManyToOneRelationshipWithUser() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setUser(user);

        assertThat(response.getUser())
                .isNotNull()
                .isEqualTo(user);
        assertThat(response.getUser().getName()).isEqualTo(user.getName());
    }

    @Test
    @DisplayName("Should handle createdAt timestamp")
    void shouldHandleCreatedAtTimestamp() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        LocalDateTime now = LocalDateTime.now();
        response.setCreatedAt(now);

        assertThat(response.getCreatedAt())
                .isNotNull()
                .isEqualTo(now);
    }

    @Test
    @DisplayName("Should store checklist ID for reference")
    void shouldStoreChecklistIdForReference() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setChecklistId(100L);

        assertThat(response.getChecklistId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should store checklist name for historical record")
    void shouldStoreChecklistNameForHistoricalRecord() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setChecklistName("Checklist de Segurança - Janeiro 2024");

        assertThat(response.getChecklistName()).isEqualTo("Checklist de Segurança - Janeiro 2024");
    }

    @Test
    @DisplayName("Should support different checklist names")
    void shouldSupportDifferentChecklistNames() {

        ChecklistResponseEntity daily = new ChecklistResponseEntity();
        daily.setChecklistName("Checklist Diário");

        ChecklistResponseEntity weekly = new ChecklistResponseEntity();
        weekly.setChecklistName("Checklist Semanal");

        ChecklistResponseEntity monthly = new ChecklistResponseEntity();
        monthly.setChecklistName("Checklist Mensal");

        assertThat(daily.getChecklistName()).isEqualTo("Checklist Diário");
        assertThat(weekly.getChecklistName()).isEqualTo("Checklist Semanal");
        assertThat(monthly.getChecklistName()).isEqualTo("Checklist Mensal");
    }

    @Test
    @DisplayName("Should associate multiple responses with same dam")
    void shouldAssociateMultipleResponsesWithSameDam() {

        ChecklistResponseEntity response1 = new ChecklistResponseEntity();
        response1.setChecklistName("Response 1");
        response1.setDam(dam);

        ChecklistResponseEntity response2 = new ChecklistResponseEntity();
        response2.setChecklistName("Response 2");
        response2.setDam(dam);

        assertThat(response1.getDam()).isEqualTo(dam);
        assertThat(response2.getDam()).isEqualTo(dam);
        assertThat(response1.getDam()).isEqualTo(response2.getDam());
    }

    @Test
    @DisplayName("Should associate multiple responses with same user")
    void shouldAssociateMultipleResponsesWithSameUser() {

        ChecklistResponseEntity response1 = new ChecklistResponseEntity();
        response1.setUser(user);

        ChecklistResponseEntity response2 = new ChecklistResponseEntity();
        response2.setUser(user);

        assertThat(response1.getUser()).isEqualTo(user);
        assertThat(response2.getUser()).isEqualTo(user);
        assertThat(response1.getUser()).isEqualTo(response2.getUser());
    }

    @Test
    @DisplayName("Should support long checklist names")
    void shouldSupportLongChecklistNames() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        String longName = "Checklist Completo de Inspeção Técnica e Segurança Estrutural da Barragem - Janeiro 2024";
        response.setChecklistName(longName);

        assertThat(response.getChecklistName()).isEqualTo(longName);
        assertThat(response.getChecklistName().length()).isGreaterThan(50);
    }

    @Test
    @DisplayName("Should handle checklist name with special characters")
    void shouldHandleChecklistNameWithSpecialCharacters() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setChecklistName("Checklist - Inspeção (2024) #001");

        assertThat(response.getChecklistName())
                .contains("-")
                .contains("(")
                .contains(")")
                .contains("#");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setId(1L);
        response.setChecklistName("Original Name");
        response.setChecklistId(100L);

        Long originalId = response.getId();

        response.setChecklistName("Updated Name");
        response.setChecklistId(200L);

        assertThat(response.getId()).isEqualTo(originalId);
        assertThat(response.getChecklistName()).isEqualTo("Updated Name");
        assertThat(response.getChecklistId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("Should handle response without questionnaire responses")
    void shouldHandleResponseWithoutQuestionnaireResponses() {

        ChecklistResponseEntity response = new ChecklistResponseEntity();
        response.setChecklistName("Empty Response");
        response.setChecklistId(100L);
        response.setDam(dam);
        response.setUser(user);

        assertThat(response.getQuestionnaireResponses()).isEmpty();
        assertThat(response.getChecklistName()).isNotBlank();
        assertThat(response.getDam()).isNotNull();
        assertThat(response.getUser()).isNotNull();
    }

    @Test
    @DisplayName("Should track different users responding to checklists")
    void shouldTrackDifferentUsersRespondingToChecklists() {

        UserEntity user1 = TestDataBuilder.user().withName("User 1").build();
        UserEntity user2 = TestDataBuilder.user().withName("User 2").build();

        ChecklistResponseEntity response1 = new ChecklistResponseEntity();
        response1.setUser(user1);

        ChecklistResponseEntity response2 = new ChecklistResponseEntity();
        response2.setUser(user2);

        assertThat(response1.getUser()).isEqualTo(user1);
        assertThat(response2.getUser()).isEqualTo(user2);
        assertThat(response1.getUser()).isNotEqualTo(response2.getUser());
    }

    @Test
    @DisplayName("Should handle multiple responses for same checklist ID")
    void shouldHandleMultipleResponsesForSameChecklistId() {

        ChecklistResponseEntity response1 = new ChecklistResponseEntity();
        response1.setChecklistId(100L);
        response1.setCreatedAt(LocalDateTime.now().minusDays(1));

        ChecklistResponseEntity response2 = new ChecklistResponseEntity();
        response2.setChecklistId(100L);
        response2.setCreatedAt(LocalDateTime.now());

        assertThat(response1.getChecklistId()).isEqualTo(response2.getChecklistId());
        assertThat(response1.getCreatedAt()).isBefore(response2.getCreatedAt());
    }
}
