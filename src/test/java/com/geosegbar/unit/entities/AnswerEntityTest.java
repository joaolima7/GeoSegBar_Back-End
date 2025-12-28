package com.geosegbar.unit.entities;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.AnswerPhotoEntity;
import com.geosegbar.entities.OptionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.QuestionnaireResponseEntity;

@DisplayName("Unit Tests - AnswerEntity")
class AnswerEntityTest extends BaseUnitTest {

    private QuestionnaireResponseEntity questionnaireResponse;
    private QuestionEntity question;

    @BeforeEach
    void setUp() {
        questionnaireResponse = new QuestionnaireResponseEntity();
        questionnaireResponse.setId(1L);

        question = new QuestionEntity();
        question.setId(1L);
        question.setQuestionText("Qual o estado da estrutura?");
    }

    @Test
    @DisplayName("Should create answer with all required fields")
    void shouldCreateAnswerWithAllRequiredFields() {

        AnswerEntity answer = new AnswerEntity();
        answer.setId(1L);
        answer.setQuestionnaireResponse(questionnaireResponse);
        answer.setQuestion(question);

        assertThat(answer).satisfies(a -> {
            assertThat(a.getId()).isEqualTo(1L);
            assertThat(a.getQuestionnaireResponse()).isEqualTo(questionnaireResponse);
            assertThat(a.getQuestion()).isEqualTo(question);
            assertThat(a.getSelectedOptions()).isNotNull().isEmpty();
            assertThat(a.getPhotos()).isNotNull().isEmpty();
        });
    }

    @Test
    @DisplayName("Should create answer with optional fields")
    void shouldCreateAnswerWithOptionalFields() {

        AnswerEntity answer = new AnswerEntity();
        answer.setComment("Estrutura apresenta rachaduras na base");
        answer.setLatitude(-23.5505);
        answer.setLongitude(-46.6333);

        assertThat(answer).satisfies(a -> {
            assertThat(a.getComment()).isEqualTo("Estrutura apresenta rachaduras na base");
            assertThat(a.getLatitude()).isEqualTo(-23.5505);
            assertThat(a.getLongitude()).isEqualTo(-46.6333);
        });
    }

    @Test
    @DisplayName("Should initialize collections as empty HashSet")
    void shouldInitializeCollectionsAsEmptyHashSet() {

        AnswerEntity answer = new AnswerEntity();

        assertThat(answer.getSelectedOptions())
                .isNotNull()
                .isInstanceOf(HashSet.class)
                .isEmpty();

        assertThat(answer.getPhotos())
                .isNotNull()
                .isInstanceOf(HashSet.class)
                .isEmpty();
    }

    @Test
    @DisplayName("Should add single option to answer")
    void shouldAddSingleOptionToAnswer() {

        AnswerEntity answer = new AnswerEntity();
        OptionEntity option = new OptionEntity();
        option.setId(1L);
        option.setLabel("Bom");
        option.setValue("Bom");

        answer.getSelectedOptions().add(option);

        assertThat(answer.getSelectedOptions())
                .hasSize(1)
                .contains(option);
    }

    @Test
    @DisplayName("Should add multiple options to answer")
    void shouldAddMultipleOptionsToAnswer() {

        AnswerEntity answer = new AnswerEntity();

        OptionEntity option1 = new OptionEntity();
        option1.setId(1L);
        option1.setLabel("Rachaduras");
        option1.setValue("Rachaduras");

        OptionEntity option2 = new OptionEntity();
        option2.setId(2L);
        option2.setLabel("Infiltração");
        option2.setValue("Infiltração");

        OptionEntity option3 = new OptionEntity();
        option3.setId(3L);
        option3.setLabel("Corrosão");
        option3.setValue("Corrosão");

        answer.getSelectedOptions().add(option1);
        answer.getSelectedOptions().add(option2);
        answer.getSelectedOptions().add(option3);

        assertThat(answer.getSelectedOptions())
                .hasSize(3)
                .containsExactlyInAnyOrder(option1, option2, option3);
    }

    @Test
    @DisplayName("Should add photo to answer")
    void shouldAddPhotoToAnswer() {

        AnswerEntity answer = new AnswerEntity();
        answer.setId(1L);

        AnswerPhotoEntity photo = new AnswerPhotoEntity();
        photo.setId(1L);
        photo.setAnswer(answer);
        photo.setImagePath("/uploads/answers/photo1.jpg");

        answer.getPhotos().add(photo);

        assertThat(answer.getPhotos())
                .hasSize(1)
                .contains(photo);
        assertThat(photo.getAnswer()).isEqualTo(answer);
    }

    @Test
    @DisplayName("Should add multiple photos to answer")
    void shouldAddMultiplePhotosToAnswer() {

        AnswerEntity answer = new AnswerEntity();
        answer.setId(1L);

        AnswerPhotoEntity photo1 = new AnswerPhotoEntity();
        photo1.setId(1L);
        photo1.setAnswer(answer);
        photo1.setImagePath("/uploads/answers/photo1.jpg");

        AnswerPhotoEntity photo2 = new AnswerPhotoEntity();
        photo2.setId(2L);
        photo2.setAnswer(answer);
        photo2.setImagePath("/uploads/answers/photo2.jpg");

        answer.getPhotos().add(photo1);
        answer.getPhotos().add(photo2);

        assertThat(answer.getPhotos())
                .hasSize(2)
                .containsExactlyInAnyOrder(photo1, photo2);
    }

    @Test
    @DisplayName("Should maintain bidirectional relationship with questionnaire response")
    void shouldMaintainBidirectionalRelationshipWithQuestionnaireResponse() {

        AnswerEntity answer = new AnswerEntity();
        answer.setId(1L);
        answer.setQuestionnaireResponse(questionnaireResponse);

        questionnaireResponse.getAnswers().add(answer);

        assertThat(answer.getQuestionnaireResponse()).isEqualTo(questionnaireResponse);
        assertThat(questionnaireResponse.getAnswers()).contains(answer);
    }

    @Test
    @DisplayName("Should validate geographic coordinates")
    void shouldValidateGeographicCoordinates() {

        AnswerEntity answer = new AnswerEntity();

        answer.setLatitude(-23.5505);
        answer.setLongitude(-46.6333);

        assertThat(answer.getLatitude()).isBetween(-90.0, 90.0);
        assertThat(answer.getLongitude()).isBetween(-180.0, 180.0);
    }

    @Test
    @DisplayName("Should allow null coordinates")
    void shouldAllowNullCoordinates() {

        AnswerEntity answer = new AnswerEntity();
        answer.setLatitude(null);
        answer.setLongitude(null);

        assertThat(answer.getLatitude()).isNull();
        assertThat(answer.getLongitude()).isNull();
    }

    @Test
    @DisplayName("Should handle comment as text")
    void shouldHandleCommentAsText() {

        String longComment = "Comentário muito longo com observações detalhadas sobre a resposta. ".repeat(50);

        AnswerEntity answer = new AnswerEntity();
        answer.setComment(longComment);

        assertThat(answer.getComment()).hasSize(longComment.length());
    }

    @Test
    @DisplayName("Should allow null comment")
    void shouldAllowNullComment() {

        AnswerEntity answer = new AnswerEntity();
        answer.setComment(null);

        assertThat(answer.getComment()).isNull();
    }

    @Test
    @DisplayName("Should create answer using all args constructor")
    void shouldCreateAnswerUsingAllArgsConstructor() {

        Set<OptionEntity> options = new HashSet<>();
        Set<AnswerPhotoEntity> photos = new HashSet<>();

        AnswerEntity answer = new AnswerEntity(
                1L,
                questionnaireResponse,
                question,
                "Comentário de teste",
                -23.5505,
                -46.6333,
                options,
                photos
        );

        assertThat(answer).satisfies(a -> {
            assertThat(a.getId()).isEqualTo(1L);
            assertThat(a.getQuestionnaireResponse()).isEqualTo(questionnaireResponse);
            assertThat(a.getQuestion()).isEqualTo(question);
            assertThat(a.getComment()).isEqualTo("Comentário de teste");
            assertThat(a.getLatitude()).isEqualTo(-23.5505);
            assertThat(a.getLongitude()).isEqualTo(-46.6333);
            assertThat(a.getSelectedOptions()).isEqualTo(options);
            assertThat(a.getPhotos()).isEqualTo(photos);
        });
    }

    @Test
    @DisplayName("Should maintain relationship with question")
    void shouldMaintainRelationshipWithQuestion() {

        AnswerEntity answer = new AnswerEntity();
        answer.setQuestion(question);

        assertThat(answer.getQuestion())
                .isNotNull()
                .isEqualTo(question);
        assertThat(answer.getQuestion().getQuestionText()).isEqualTo("Qual o estado da estrutura?");
    }

    @Test
    @DisplayName("Should support answer without options")
    void shouldSupportAnswerWithoutOptions() {

        AnswerEntity answer = new AnswerEntity();
        answer.setQuestion(question);
        answer.setComment("Resposta em texto livre");

        assertThat(answer.getSelectedOptions()).isEmpty();
        assertThat(answer.getComment()).isNotEmpty();
    }

    @Test
    @DisplayName("Should support answer with options and comment")
    void shouldSupportAnswerWithOptionsAndComment() {

        AnswerEntity answer = new AnswerEntity();

        OptionEntity option = new OptionEntity();
        option.setLabel("Regular");
        answer.getSelectedOptions().add(option);
        answer.setComment("Precisa de manutenção preventiva");

        assertThat(answer.getSelectedOptions()).hasSize(1);
        assertThat(answer.getComment()).isEqualTo("Precisa de manutenção preventiva");
    }

    @Test
    @DisplayName("Should support answer with coordinates and photos")
    void shouldSupportAnswerWithCoordinatesAndPhotos() {

        AnswerEntity answer = new AnswerEntity();
        answer.setLatitude(-23.5505);
        answer.setLongitude(-46.6333);

        AnswerPhotoEntity photo = new AnswerPhotoEntity();
        photo.setImagePath("/uploads/answers/location.jpg");
        photo.setAnswer(answer);
        answer.getPhotos().add(photo);

        assertThat(answer.getLatitude()).isNotNull();
        assertThat(answer.getLongitude()).isNotNull();
        assertThat(answer.getPhotos()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle empty comment")
    void shouldHandleEmptyComment() {

        AnswerEntity answer = new AnswerEntity();
        answer.setComment("");

        assertThat(answer.getComment()).isEmpty();
    }

    @Test
    @DisplayName("Should create answer with no args constructor")
    void shouldCreateAnswerWithNoArgsConstructor() {

        AnswerEntity answer = new AnswerEntity();

        assertThat(answer).isNotNull();
        assertThat(answer.getId()).isNull();
        assertThat(answer.getQuestionnaireResponse()).isNull();
        assertThat(answer.getQuestion()).isNull();
        assertThat(answer.getComment()).isNull();
        assertThat(answer.getLatitude()).isNull();
        assertThat(answer.getLongitude()).isNull();
        assertThat(answer.getSelectedOptions()).isNotNull().isEmpty();
        assertThat(answer.getPhotos()).isNotNull().isEmpty();
    }
}
