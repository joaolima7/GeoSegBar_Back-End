// package com.geosegbar.unit.infra.checklist_submission.services;

// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.Optional;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatCode;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import static org.mockito.ArgumentMatchers.any;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import static org.mockito.Mockito.never;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;
// import org.mockito.junit.jupiter.MockitoExtension;

// import com.geosegbar.config.BaseUnitTest;
// import com.geosegbar.entities.OptionEntity;
// import com.geosegbar.exceptions.InvalidInputException;
// import com.geosegbar.exceptions.NotFoundException;
// import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;
// import com.geosegbar.infra.checklist_submission.dtos.PhotoSubmissionDTO;
// import com.geosegbar.infra.checklist_submission.services.PVAnswerValidator;
// import com.geosegbar.infra.option.persistence.jpa.OptionRepository;

// @ExtendWith(MockitoExtension.class)
// @DisplayName("PVAnswerValidator Unit Tests")
// class PVAnswerValidatorTest extends BaseUnitTest {

//     @Mock
//     private OptionRepository optionRepository;

//     @InjectMocks
//     private PVAnswerValidator pvAnswerValidator;

//     private OptionEntity pvOption;
//     private OptionEntity normalOption;
//     private AnswerSubmissionDTO answerDto;

//     @BeforeEach
//     void setUp() {
//         pvOption = new OptionEntity();
//         pvOption.setId(1L);
//         pvOption.setLabel("PV");

//         normalOption = new OptionEntity();
//         normalOption.setId(2L);
//         normalOption.setLabel("OK");

//         answerDto = new AnswerSubmissionDTO();
//         answerDto.setSelectedOptionIds(new ArrayList<>());
//     }

//     // ==================== validatePVAnswer Tests ====================
//     @Test
//     @DisplayName("Should validate PV answer with all required fields successfully")
//     void shouldValidatePVAnswerWithAllRequiredFields() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         answerDto.setAnomalyRecommendation("Fix immediately");
//         answerDto.setAnomalyDangerLevelId(1L);
//         answerDto.setAnomalyStatusId(1L);
//         answerDto.setPhotos(Arrays.asList(new PhotoSubmissionDTO()));
//         answerDto.setLatitude(-23.5505);
//         answerDto.setLongitude(-46.6333);

//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then - Should not throw exception
//         assertThatCode(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .doesNotThrowAnyException();

//         verify(optionRepository).findById(1L);
//     }

//     @Test
//     @DisplayName("Should throw InvalidInputException when PV answer missing recommendation")
//     void shouldThrowInvalidInputExceptionWhenMissingRecommendation() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         answerDto.setAnomalyDangerLevelId(1L);
//         answerDto.setAnomalyStatusId(1L);
//         answerDto.setPhotos(Arrays.asList(new PhotoSubmissionDTO()));
//         answerDto.setLatitude(-23.5505);
//         answerDto.setLongitude(-46.6333);
//         // Missing recommendation

//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .isInstanceOf(InvalidInputException.class)
//                 .hasMessageContaining("Test Question")
//                 .hasMessageContaining("foi marcada como PV")
//                 .hasMessageContaining("recommendation");

//         verify(optionRepository).findById(1L);
//     }

//     @Test
//     @DisplayName("Should throw InvalidInputException when PV answer has empty recommendation")
//     void shouldThrowInvalidInputExceptionWhenRecommendationEmpty() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         answerDto.setAnomalyRecommendation("   "); // Only whitespace
//         answerDto.setAnomalyDangerLevelId(1L);
//         answerDto.setAnomalyStatusId(1L);
//         answerDto.setPhotos(Arrays.asList(new PhotoSubmissionDTO()));
//         answerDto.setLatitude(-23.5505);
//         answerDto.setLongitude(-46.6333);

//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .isInstanceOf(InvalidInputException.class)
//                 .hasMessageContaining("recommendation");
//     }

//     @Test
//     @DisplayName("Should throw InvalidInputException when PV answer missing danger level")
//     void shouldThrowInvalidInputExceptionWhenMissingDangerLevel() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         answerDto.setAnomalyRecommendation("Fix immediately");
//         answerDto.setAnomalyStatusId(1L);
//         answerDto.setPhotos(Arrays.asList(new PhotoSubmissionDTO()));
//         answerDto.setLatitude(-23.5505);
//         answerDto.setLongitude(-46.6333);
//         // Missing dangerLevelId

//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .isInstanceOf(InvalidInputException.class)
//                 .hasMessageContaining("dangerLevelId");
//     }

//     @Test
//     @DisplayName("Should throw InvalidInputException when PV answer missing status")
//     void shouldThrowInvalidInputExceptionWhenMissingStatus() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         answerDto.setAnomalyRecommendation("Fix immediately");
//         answerDto.setAnomalyDangerLevelId(1L);
//         answerDto.setPhotos(Arrays.asList(new PhotoSubmissionDTO()));
//         answerDto.setLatitude(-23.5505);
//         answerDto.setLongitude(-46.6333);
//         // Missing statusId

//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .isInstanceOf(InvalidInputException.class)
//                 .hasMessageContaining("statusId");
//     }

//     @Test
//     @DisplayName("Should throw InvalidInputException when PV answer missing photos")
//     void shouldThrowInvalidInputExceptionWhenMissingPhotos() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         answerDto.setAnomalyRecommendation("Fix immediately");
//         answerDto.setAnomalyDangerLevelId(1L);
//         answerDto.setAnomalyStatusId(1L);
//         answerDto.setLatitude(-23.5505);
//         answerDto.setLongitude(-46.6333);
//         // Missing photos

//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .isInstanceOf(InvalidInputException.class)
//                 .hasMessageContaining("photo");
//     }

//     @Test
//     @DisplayName("Should throw InvalidInputException when PV answer has empty photos list")
//     void shouldThrowInvalidInputExceptionWhenPhotosListEmpty() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         answerDto.setAnomalyRecommendation("Fix immediately");
//         answerDto.setAnomalyDangerLevelId(1L);
//         answerDto.setAnomalyStatusId(1L);
//         answerDto.setPhotos(new ArrayList<>()); // Empty list
//         answerDto.setLatitude(-23.5505);
//         answerDto.setLongitude(-46.6333);

//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .isInstanceOf(InvalidInputException.class)
//                 .hasMessageContaining("photo");
//     }

//     @Test
//     @DisplayName("Should throw InvalidInputException when PV answer missing latitude")
//     void shouldThrowInvalidInputExceptionWhenMissingLatitude() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         answerDto.setAnomalyRecommendation("Fix immediately");
//         answerDto.setAnomalyDangerLevelId(1L);
//         answerDto.setAnomalyStatusId(1L);
//         answerDto.setPhotos(Arrays.asList(new PhotoSubmissionDTO()));
//         answerDto.setLongitude(-46.6333);
//         // Missing latitude

//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .isInstanceOf(InvalidInputException.class)
//                 .hasMessageContaining("latitude/longitude");
//     }

//     @Test
//     @DisplayName("Should throw InvalidInputException when PV answer missing longitude")
//     void shouldThrowInvalidInputExceptionWhenMissingLongitude() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         answerDto.setAnomalyRecommendation("Fix immediately");
//         answerDto.setAnomalyDangerLevelId(1L);
//         answerDto.setAnomalyStatusId(1L);
//         answerDto.setPhotos(Arrays.asList(new PhotoSubmissionDTO()));
//         answerDto.setLatitude(-23.5505);
//         // Missing longitude

//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .isInstanceOf(InvalidInputException.class)
//                 .hasMessageContaining("latitude/longitude");
//     }

//     @Test
//     @DisplayName("Should throw InvalidInputException with all missing fields listed")
//     void shouldThrowInvalidInputExceptionWithAllMissingFieldsListed() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         // All required fields missing

//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .isInstanceOf(InvalidInputException.class)
//                 .hasMessageContaining("recommendation")
//                 .hasMessageContaining("dangerLevelId")
//                 .hasMessageContaining("statusId")
//                 .hasMessageContaining("photo")
//                 .hasMessageContaining("latitude/longitude");
//     }

//     @Test
//     @DisplayName("Should not validate when option is not PV")
//     void shouldNotValidateWhenOptionIsNotPV() {
//         // Given
//         answerDto.getSelectedOptionIds().add(2L);
//         // No required PV fields

//         when(optionRepository.findById(2L)).thenReturn(Optional.of(normalOption));

//         // When/Then - Should not throw exception
//         assertThatCode(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .doesNotThrowAnyException();

//         verify(optionRepository).findById(2L);
//     }

//     @Test
//     @DisplayName("Should validate when multiple options and one is PV")
//     void shouldValidateWhenMultipleOptionsAndOneIsPV() {
//         // Given
//         answerDto.getSelectedOptionIds().add(2L);
//         answerDto.getSelectedOptionIds().add(1L); // PV option
//         answerDto.setAnomalyRecommendation("Fix immediately");
//         answerDto.setAnomalyDangerLevelId(1L);
//         answerDto.setAnomalyStatusId(1L);
//         answerDto.setPhotos(Arrays.asList(new PhotoSubmissionDTO()));
//         answerDto.setLatitude(-23.5505);
//         answerDto.setLongitude(-46.6333);

//         when(optionRepository.findById(2L)).thenReturn(Optional.of(normalOption));
//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When/Then
//         assertThatCode(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .doesNotThrowAnyException();
//     }

//     @Test
//     @DisplayName("Should throw NotFoundException when option not found")
//     void shouldThrowNotFoundExceptionWhenOptionNotFound() {
//         // Given
//         answerDto.getSelectedOptionIds().add(999L);

//         when(optionRepository.findById(999L)).thenReturn(Optional.empty());

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .isInstanceOf(NotFoundException.class)
//                 .hasMessageContaining("Opção não encontrada: 999");

//         verify(optionRepository).findById(999L);
//     }

//     @Test
//     @DisplayName("Should not validate when selectedOptionIds is null")
//     void shouldNotValidateWhenSelectedOptionIdsIsNull() {
//         // Given
//         answerDto.setSelectedOptionIds(null);

//         // When/Then - Should not throw exception
//         assertThatCode(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .doesNotThrowAnyException();

//         verify(optionRepository, never()).findById(any());
//     }

//     @Test
//     @DisplayName("Should not validate when selectedOptionIds is empty")
//     void shouldNotValidateWhenSelectedOptionIdsIsEmpty() {
//         // Given
//         answerDto.setSelectedOptionIds(new ArrayList<>());

//         // When/Then - Should not throw exception
//         assertThatCode(() -> pvAnswerValidator.validatePVAnswer(answerDto, "Test Question"))
//                 .doesNotThrowAnyException();

//         verify(optionRepository, never()).findById(any());
//     }

//     // ==================== isPVAnswer Tests ====================
//     @Test
//     @DisplayName("Should return true when answer has PV option")
//     void shouldReturnTrueWhenAnswerHasPVOption() {
//         // Given
//         answerDto.getSelectedOptionIds().add(1L);
//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When
//         boolean result = pvAnswerValidator.isPVAnswer(answerDto);

//         // Then
//         assertThat(result).isTrue();
//         verify(optionRepository).findById(1L);
//     }

//     @Test
//     @DisplayName("Should return false when answer has no PV option")
//     void shouldReturnFalseWhenAnswerHasNoPVOption() {
//         // Given
//         answerDto.getSelectedOptionIds().add(2L);
//         when(optionRepository.findById(2L)).thenReturn(Optional.of(normalOption));

//         // When
//         boolean result = pvAnswerValidator.isPVAnswer(answerDto);

//         // Then
//         assertThat(result).isFalse();
//         verify(optionRepository).findById(2L);
//     }

//     @Test
//     @DisplayName("Should return true when multiple options and one is PV")
//     void shouldReturnTrueWhenMultipleOptionsAndOneIsPV() {
//         // Given
//         answerDto.getSelectedOptionIds().add(2L);
//         answerDto.getSelectedOptionIds().add(1L);
//         when(optionRepository.findById(2L)).thenReturn(Optional.of(normalOption));
//         when(optionRepository.findById(1L)).thenReturn(Optional.of(pvOption));

//         // When
//         boolean result = pvAnswerValidator.isPVAnswer(answerDto);

//         // Then
//         assertThat(result).isTrue();
//         verify(optionRepository).findById(2L);
//         verify(optionRepository).findById(1L);
//     }

//     @Test
//     @DisplayName("Should return false when selectedOptionIds is null")
//     void shouldReturnFalseWhenSelectedOptionIdsIsNull() {
//         // Given
//         answerDto.setSelectedOptionIds(null);

//         // When
//         boolean result = pvAnswerValidator.isPVAnswer(answerDto);

//         // Then
//         assertThat(result).isFalse();
//         verify(optionRepository, never()).findById(any());
//     }

//     @Test
//     @DisplayName("Should return false when selectedOptionIds is empty")
//     void shouldReturnFalseWhenSelectedOptionIdsIsEmpty() {
//         // Given
//         answerDto.setSelectedOptionIds(new ArrayList<>());

//         // When
//         boolean result = pvAnswerValidator.isPVAnswer(answerDto);

//         // Then
//         assertThat(result).isFalse();
//         verify(optionRepository, never()).findById(any());
//     }

//     @Test
//     @DisplayName("Should throw NotFoundException when option not found in isPVAnswer")
//     void shouldThrowNotFoundExceptionWhenOptionNotFoundInIsPVAnswer() {
//         // Given
//         answerDto.getSelectedOptionIds().add(999L);
//         when(optionRepository.findById(999L)).thenReturn(Optional.empty());

//         // When/Then
//         assertThatThrownBy(() -> pvAnswerValidator.isPVAnswer(answerDto))
//                 .isInstanceOf(NotFoundException.class)
//                 .hasMessageContaining("Opção não encontrada: 999");
//     }
// }
