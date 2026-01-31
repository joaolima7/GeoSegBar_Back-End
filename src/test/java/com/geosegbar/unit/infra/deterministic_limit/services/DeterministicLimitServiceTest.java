// package com.geosegbar.unit.infra.deterministic_limit.services;

// import java.util.Arrays;
// import java.util.Collections;
// import java.util.List;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertNull;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import static org.mockito.ArgumentMatchers.any;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import static org.mockito.Mockito.never;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;
// import org.mockito.junit.jupiter.MockitoExtension;

// import com.geosegbar.entities.DeterministicLimitEntity;
// import com.geosegbar.entities.OutputEntity;
// import com.geosegbar.exceptions.NotFoundException;
// import com.geosegbar.infra.deterministic_limit.persistence.jpa.DeterministicLimitRepository;
// import com.geosegbar.infra.deterministic_limit.services.DeterministicLimitService;
// import com.geosegbar.infra.output.persistence.jpa.OutputRepository;

// @ExtendWith(MockitoExtension.class)
// @Tag("unit")
// @DisplayName("DeterministicLimitService Unit Tests")
// class DeterministicLimitServiceTest {

//     @Mock
//     private DeterministicLimitRepository deterministicLimitRepository;

//     @Mock
//     private OutputRepository outputRepository;

//     @InjectMocks
//     private DeterministicLimitService service;

//     private DeterministicLimitEntity limitEntity;
//     private OutputEntity outputEntity;

//     @BeforeEach
//     void setUp() {
//         outputEntity = new OutputEntity();
//         outputEntity.setId(1L);
//         outputEntity.setName("Test Output");

//         limitEntity = new DeterministicLimitEntity();
//         limitEntity.setId(1L);
//         limitEntity.setAttentionValue(10.0);
//         limitEntity.setAlertValue(20.0);
//         limitEntity.setEmergencyValue(30.0);
//         limitEntity.setOutput(outputEntity);
//     }

//     @Test
//     @DisplayName("Should find deterministic limit by output ID successfully")
//     void shouldFindByOutputIdSuccessfully() {
//         when(deterministicLimitRepository.findByOutputId(1L)).thenReturn(Optional.of(limitEntity));

//         Optional<DeterministicLimitEntity> result = service.findByOutputId(1L);

//         assertTrue(result.isPresent());
//         assertEquals(1L, result.get().getId());
//         assertEquals(10.0, result.get().getAttentionValue());
//         verify(deterministicLimitRepository).findByOutputId(1L);
//     }

//     @Test
//     @DisplayName("Should return empty when deterministic limit not found by output ID")
//     void shouldReturnEmptyWhenLimitNotFoundByOutputId() {
//         when(deterministicLimitRepository.findByOutputId(1L)).thenReturn(Optional.empty());

//         Optional<DeterministicLimitEntity> result = service.findByOutputId(1L);

//         assertFalse(result.isPresent());
//         verify(deterministicLimitRepository).findByOutputId(1L);
//     }

//     @Test
//     @DisplayName("Should find deterministic limit IDs by dam ID successfully")
//     void shouldFindLimitIdsByDamIdSuccessfully() {
//         when(deterministicLimitRepository.findLimitIdsByOutputInstrumentDamId(1L))
//                 .thenReturn(Arrays.asList(1L, 2L, 3L));

//         List<Long> results = service.findDeterministicLimitIdsByOutputInstrumentDamId(1L);

//         assertNotNull(results);
//         assertEquals(3, results.size());
//         assertTrue(results.contains(1L));
//         assertTrue(results.contains(2L));
//         assertTrue(results.contains(3L));
//         verify(deterministicLimitRepository).findLimitIdsByOutputInstrumentDamId(1L);
//     }

//     @Test
//     @DisplayName("Should return empty list when no limit IDs found for dam")
//     void shouldReturnEmptyListWhenNoLimitIdsFoundForDam() {
//         when(deterministicLimitRepository.findLimitIdsByOutputInstrumentDamId(1L))
//                 .thenReturn(Collections.emptyList());

//         List<Long> results = service.findDeterministicLimitIdsByOutputInstrumentDamId(1L);

//         assertNotNull(results);
//         assertTrue(results.isEmpty());
//         verify(deterministicLimitRepository).findLimitIdsByOutputInstrumentDamId(1L);
//     }

//     @Test
//     @DisplayName("Should find deterministic limit by ID successfully")
//     void shouldFindByIdSuccessfully() {
//         when(deterministicLimitRepository.findById(1L)).thenReturn(Optional.of(limitEntity));

//         DeterministicLimitEntity result = service.findById(1L);

//         assertNotNull(result);
//         assertEquals(1L, result.getId());
//         assertEquals(10.0, result.getAttentionValue());
//         assertEquals(20.0, result.getAlertValue());
//         assertEquals(30.0, result.getEmergencyValue());
//         verify(deterministicLimitRepository).findById(1L);
//     }

//     @Test
//     @DisplayName("Should throw NotFoundException when deterministic limit not found by ID")
//     void shouldThrowNotFoundExceptionWhenLimitNotFoundById() {
//         when(deterministicLimitRepository.findById(1L)).thenReturn(Optional.empty());

//         NotFoundException exception = assertThrows(NotFoundException.class, () -> service.findById(1L));

//         assertTrue(exception.getMessage().contains("Limite determinístico não encontrado com ID: 1"));
//         verify(deterministicLimitRepository).findById(1L);
//     }

//     @Test
//     @DisplayName("Should delete deterministic limit and clear output reference")
//     void shouldDeleteLimitAndClearOutputReference() {
//         when(deterministicLimitRepository.findById(1L)).thenReturn(Optional.of(limitEntity));
//         when(outputRepository.save(any(OutputEntity.class))).thenReturn(outputEntity);

//         service.deleteById(1L);

//         verify(deterministicLimitRepository).findById(1L);
//         verify(outputRepository).save(outputEntity);
//         verify(deterministicLimitRepository).delete(limitEntity);
//         assertNull(outputEntity.getDeterministicLimit());
//     }

//     @Test
//     @DisplayName("Should throw NotFoundException when deleting non-existent limit")
//     void shouldThrowNotFoundExceptionWhenDeletingNonExistentLimit() {
//         when(deterministicLimitRepository.findById(1L)).thenReturn(Optional.empty());

//         assertThrows(NotFoundException.class, () -> service.deleteById(1L));

//         verify(deterministicLimitRepository).findById(1L);
//         verify(deterministicLimitRepository, never()).delete(any());
//     }
// }
