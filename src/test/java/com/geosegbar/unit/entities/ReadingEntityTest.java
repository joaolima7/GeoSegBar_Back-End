// package com.geosegbar.unit.entities;

// import java.time.LocalDate;
// import java.time.LocalTime;

// import static org.assertj.core.api.Assertions.assertThat;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;

// import com.geosegbar.common.enums.LimitStatusEnum;
// import com.geosegbar.config.BaseUnitTest;
// import com.geosegbar.entities.InstrumentEntity;
// import com.geosegbar.entities.OutputEntity;
// import com.geosegbar.entities.ReadingEntity;
// import com.geosegbar.entities.ReadingInputValueEntity;
// import com.geosegbar.entities.UserEntity;
// import com.geosegbar.fixtures.TestDataBuilder;

// @DisplayName("Unit Tests - ReadingEntity")
// class ReadingEntityTest extends BaseUnitTest {

//     private InstrumentEntity instrument;
//     private OutputEntity output;
//     private UserEntity user;

//     @BeforeEach
//     void setUp() {
//         TestDataBuilder.resetIdGenerator();

//         instrument = new InstrumentEntity();
//         instrument.setId(1L);
//         instrument.setName("Piezômetro PZ-01");

//         output = new OutputEntity();
//         output.setId(1L);
//         output.setName("Pressão");

//         user = new UserEntity();
//         user.setId(1L);
//         user.setName("User Test");
//     }

//     @Test
//     @DisplayName("Should create reading with all required fields")
//     void shouldCreateReadingWithAllRequiredFields() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setId(1L);
//         reading.setDate(LocalDate.of(2024, 12, 28));
//         reading.setHour(LocalTime.of(10, 30));
//         reading.setCalculatedValue(25.5);
//         reading.setLimitStatus(LimitStatusEnum.NORMAL);
//         reading.setActive(true);
//         reading.setInstrument(instrument);
//         reading.setOutput(output);

//         assertThat(reading).satisfies(r -> {
//             assertThat(r.getId()).isEqualTo(1L);
//             assertThat(r.getDate()).isEqualTo(LocalDate.of(2024, 12, 28));
//             assertThat(r.getHour()).isEqualTo(LocalTime.of(10, 30));
//             assertThat(r.getCalculatedValue()).isEqualTo(25.5);
//             assertThat(r.getLimitStatus()).isEqualTo(LimitStatusEnum.NORMAL);
//             assertThat(r.getActive()).isTrue();
//             assertThat(r.getInstrument()).isEqualTo(instrument);
//             assertThat(r.getOutput()).isEqualTo(output);
//         });
//     }

//     @Test
//     @DisplayName("Should create using all args constructor")
//     void shouldCreateUsingAllArgsConstructor() {

//         ReadingEntity reading = new ReadingEntity(
//                 1L,
//                 LocalDate.of(2024, 12, 28),
//                 LocalTime.of(14, 45),
//                 30.2,
//                 LimitStatusEnum.ATENCAO,
//                 true,
//                 "Leitura com observação",
//                 user,
//                 instrument,
//                 output,
//                 null
//         );

//         assertThat(reading.getId()).isEqualTo(1L);
//         assertThat(reading.getDate()).isEqualTo(LocalDate.of(2024, 12, 28));
//         assertThat(reading.getHour()).isEqualTo(LocalTime.of(14, 45));
//         assertThat(reading.getCalculatedValue()).isEqualTo(30.2);
//         assertThat(reading.getLimitStatus()).isEqualTo(LimitStatusEnum.ATENCAO);
//         assertThat(reading.getActive()).isTrue();
//         assertThat(reading.getComment()).isEqualTo("Leitura com observação");
//         assertThat(reading.getUser()).isEqualTo(user);
//         assertThat(reading.getInstrument()).isEqualTo(instrument);
//         assertThat(reading.getOutput()).isEqualTo(output);
//     }

//     @Test
//     @DisplayName("Should maintain ManyToOne relationship with Instrument")
//     void shouldMaintainManyToOneRelationshipWithInstrument() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setInstrument(instrument);

//         assertThat(reading.getInstrument())
//                 .isNotNull()
//                 .isEqualTo(instrument);
//     }

//     @Test
//     @DisplayName("Should maintain ManyToOne relationship with Output")
//     void shouldMaintainManyToOneRelationshipWithOutput() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setOutput(output);

//         assertThat(reading.getOutput())
//                 .isNotNull()
//                 .isEqualTo(output);
//     }

//     @Test
//     @DisplayName("Should maintain ManyToOne relationship with User")
//     void shouldMaintainManyToOneRelationshipWithUser() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setUser(user);

//         assertThat(reading.getUser())
//                 .isNotNull()
//                 .isEqualTo(user);
//     }

//     @Test
//     @DisplayName("Should allow null user for automated readings")
//     void shouldAllowNullUserForAutomatedReadings() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setDate(LocalDate.now());
//         reading.setHour(LocalTime.now());
//         reading.setUser(null);

//         assertThat(reading.getUser()).isNull();
//     }

//     @Test
//     @DisplayName("Should support LimitStatusEnum NORMAL")
//     void shouldSupportLimitStatusEnumNormal() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setLimitStatus(LimitStatusEnum.NORMAL);

//         assertThat(reading.getLimitStatus()).isEqualTo(LimitStatusEnum.NORMAL);
//     }

//     @Test
//     @DisplayName("Should support LimitStatusEnum ATENCAO")
//     void shouldSupportLimitStatusEnumAtencao() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setLimitStatus(LimitStatusEnum.ATENCAO);

//         assertThat(reading.getLimitStatus()).isEqualTo(LimitStatusEnum.ATENCAO);
//     }

//     @Test
//     @DisplayName("Should support LimitStatusEnum ALERTA")
//     void shouldSupportLimitStatusEnumAlerta() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setLimitStatus(LimitStatusEnum.ALERTA);

//         assertThat(reading.getLimitStatus()).isEqualTo(LimitStatusEnum.ALERTA);
//     }

//     @Test
//     @DisplayName("Should support LimitStatusEnum EMERGENCIA")
//     void shouldSupportLimitStatusEnumEmergencia() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setLimitStatus(LimitStatusEnum.EMERGENCIA);

//         assertThat(reading.getLimitStatus()).isEqualTo(LimitStatusEnum.EMERGENCIA);
//     }

//     @Test
//     @DisplayName("Should support active flag true")
//     void shouldSupportActiveFlagTrue() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setActive(true);

//         assertThat(reading.getActive()).isTrue();
//     }

//     @Test
//     @DisplayName("Should support active flag false for invalidated readings")
//     void shouldSupportActiveFlagFalseForInvalidatedReadings() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setActive(false);

//         assertThat(reading.getActive()).isFalse();
//     }

//     @Test
//     @DisplayName("Should support optional comment field")
//     void shouldSupportOptionalCommentField() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setComment("Observação importante sobre a leitura");

//         assertThat(reading.getComment()).isEqualTo("Observação importante sobre a leitura");
//     }

//     @Test
//     @DisplayName("Should allow null comment")
//     void shouldAllowNullComment() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setComment(null);

//         assertThat(reading.getComment()).isNull();
//     }

//     @Test
//     @DisplayName("Should support long comments as TEXT")
//     void shouldSupportLongCommentsAsText() {

//         String longComment = "Este é um comentário muito longo que descreve em detalhes "
//                 + "todas as observações feitas durante a leitura do instrumento, "
//                 + "incluindo condições climáticas, estado do equipamento e "
//                 + "qualquer outra informação relevante para a análise posterior. "
//                 + "Este tipo de comentário extenso é importante para manter "
//                 + "um histórico completo das inspeções realizadas.";
//         ReadingEntity reading = new ReadingEntity();
//         reading.setComment(longComment);

//         assertThat(reading.getComment()).hasSize(342);
//     }

//     @Test
//     @DisplayName("Should maintain OneToMany collection of input values")
//     void shouldMaintainOneToManyCollectionOfInputValues() {

//         ReadingEntity reading = new ReadingEntity();

//         ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
//         inputValue.setId(1L);
//         inputValue.setInputAcronym("X");

//         reading.getInputValues().add(inputValue);

//         assertThat(reading.getInputValues())
//                 .isNotNull()
//                 .hasSize(1)
//                 .contains(inputValue);
//     }

//     @Test
//     @DisplayName("Should support multiple input values per reading")
//     void shouldSupportMultipleInputValuesPerReading() {

//         ReadingEntity reading = new ReadingEntity();

//         ReadingInputValueEntity input1 = new ReadingInputValueEntity();
//         input1.setId(1L);
//         input1.setInputAcronym("X");

//         ReadingInputValueEntity input2 = new ReadingInputValueEntity();
//         input2.setId(2L);
//         input2.setInputAcronym("Y");

//         ReadingInputValueEntity input3 = new ReadingInputValueEntity();
//         input3.setId(3L);
//         input3.setInputAcronym("Z");

//         reading.getInputValues().add(input1);
//         reading.getInputValues().add(input2);
//         reading.getInputValues().add(input3);

//         assertThat(reading.getInputValues()).hasSize(3);
//     }

//     @Test
//     @DisplayName("Should initialize empty input values collection by default")
//     void shouldInitializeEmptyInputValuesCollectionByDefault() {

//         ReadingEntity reading = new ReadingEntity();

//         assertThat(reading.getInputValues()).isNotNull().isEmpty();
//     }

//     @Test
//     @DisplayName("Should support date tracking")
//     void shouldSupportDateTracking() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setDate(LocalDate.of(2024, 12, 28));

//         assertThat(reading.getDate()).isEqualTo(LocalDate.of(2024, 12, 28));
//     }

//     @Test
//     @DisplayName("Should support time tracking with minutes precision")
//     void shouldSupportTimeTrackingWithMinutesPrecision() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setHour(LocalTime.of(14, 30));

//         assertThat(reading.getHour()).isEqualTo(LocalTime.of(14, 30));
//     }

//     @Test
//     @DisplayName("Should support time tracking with seconds precision")
//     void shouldSupportTimeTrackingWithSecondsPrecision() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setHour(LocalTime.of(14, 30, 45));

//         assertThat(reading.getHour()).isEqualTo(LocalTime.of(14, 30, 45));
//     }

//     @Test
//     @DisplayName("Should support positive calculated values")
//     void shouldSupportPositiveCalculatedValues() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setCalculatedValue(123.45);

//         assertThat(reading.getCalculatedValue()).isEqualTo(123.45);
//     }

//     @Test
//     @DisplayName("Should support negative calculated values")
//     void shouldSupportNegativeCalculatedValues() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setCalculatedValue(-15.8);

//         assertThat(reading.getCalculatedValue()).isEqualTo(-15.8);
//     }

//     @Test
//     @DisplayName("Should support zero calculated value")
//     void shouldSupportZeroCalculatedValue() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setCalculatedValue(0.0);

//         assertThat(reading.getCalculatedValue()).isZero();
//     }

//     @Test
//     @DisplayName("Should support high precision calculated values")
//     void shouldSupportHighPrecisionCalculatedValues() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setCalculatedValue(123.456789);

//         assertThat(reading.getCalculatedValue()).isEqualTo(123.456789);
//     }

//     @Test
//     @DisplayName("Should maintain identity through property changes")
//     void shouldMaintainIdentityThroughPropertyChanges() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setId(1L);
//         reading.setCalculatedValue(10.0);

//         Long originalId = reading.getId();

//         reading.setCalculatedValue(20.0);
//         reading.setLimitStatus(LimitStatusEnum.ALERTA);

//         assertThat(reading.getId()).isEqualTo(originalId);
//     }

//     @Test
//     @DisplayName("Should support cascade operations on input values")
//     void shouldSupportCascadeOperationsOnInputValues() {

//         ReadingEntity reading = new ReadingEntity();
//         ReadingInputValueEntity inputValue = new ReadingInputValueEntity();

//         reading.getInputValues().add(inputValue);

//         assertThat(reading.getInputValues()).hasSize(1);
//     }

//     @Test
//     @DisplayName("Should support orphan removal for input values")
//     void shouldSupportOrphanRemovalForInputValues() {

//         ReadingEntity reading = new ReadingEntity();
//         ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
//         reading.getInputValues().add(inputValue);

//         reading.getInputValues().remove(inputValue);

//         assertThat(reading.getInputValues()).isEmpty();
//     }

//     @Test
//     @DisplayName("Should support multiple readings per instrument")
//     void shouldSupportMultipleReadingsPerInstrument() {

//         ReadingEntity reading1 = new ReadingEntity();
//         reading1.setId(1L);
//         reading1.setInstrument(instrument);
//         reading1.setDate(LocalDate.of(2024, 12, 27));

//         ReadingEntity reading2 = new ReadingEntity();
//         reading2.setId(2L);
//         reading2.setInstrument(instrument);
//         reading2.setDate(LocalDate.of(2024, 12, 28));

//         assertThat(reading1.getInstrument()).isEqualTo(reading2.getInstrument());
//         assertThat(reading1.getId()).isNotEqualTo(reading2.getId());
//     }

//     @Test
//     @DisplayName("Should support Portuguese characters in comment")
//     void shouldSupportPortugueseCharactersInComment() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setComment("Observação com acentuação: água, condição, técnico");

//         assertThat(reading.getComment()).contains("á", "ã", "ç", "é");
//     }

//     @Test
//     @DisplayName("Should support lazy fetch for input values")
//     void shouldSupportLazyFetchForInputValues() {

//         ReadingEntity reading = new ReadingEntity();

//         assertThat(reading.getInputValues()).isNotNull();
//     }

//     @Test
//     @DisplayName("Should support bidirectional relationship with input values")
//     void shouldSupportBidirectionalRelationshipWithInputValues() {

//         ReadingEntity reading = new ReadingEntity();
//         reading.setId(1L);

//         ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
//         inputValue.setId(1L);
//         inputValue.setReading(reading);

//         reading.getInputValues().add(inputValue);

//         assertThat(inputValue.getReading()).isEqualTo(reading);
//         assertThat(reading.getInputValues()).contains(inputValue);
//     }
// }
