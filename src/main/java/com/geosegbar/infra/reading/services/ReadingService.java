package com.geosegbar.infra.reading.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.ReadingEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;
import com.geosegbar.infra.reading.dtos.ReadingResponseDTO;
import com.geosegbar.infra.reading.persistence.jpa.ReadingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingService {

    private final ReadingRepository readingRepository;
    private final InstrumentRepository instrumentRepository;

    public List<ReadingResponseDTO> findByInstrumentId(Long instrumentId) {
        List<ReadingEntity> readings = readingRepository.findByInstrumentId(instrumentId);
        return readings.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public Page<ReadingResponseDTO> findByInstrumentId(Long instrumentId, Pageable pageable) {
        Page<ReadingEntity> readings = readingRepository.findByInstrumentId(instrumentId, pageable);
        return readings.map(this::mapToResponseDTO);
    }

    public Page<ReadingResponseDTO> findByFilters(Long instrumentId, LocalDate startDate, LocalDate endDate,
            LimitStatusEnum limitStatus, Pageable pageable) {
        Page<ReadingEntity> readings = readingRepository.findByFilters(instrumentId, startDate, endDate, limitStatus, pageable);
        return readings.map(this::mapToResponseDTO);
    }

    public ReadingEntity findById(Long id) {
        return readingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leitura não encontrada com ID: " + id));
    }

    @Transactional
    public ReadingResponseDTO create(Long instrumentId, ReadingRequestDTO request) {
        InstrumentEntity instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));

        ReadingEntity reading = new ReadingEntity();
        reading.setDate(request.getDate());
        reading.setHour(request.getHour());
        reading.setValue(request.getValue());
        reading.setInstrument(instrument);

        LimitStatusEnum limitStatus = determineLimitStatus(instrument, request.getValue());
        reading.setLimitStatus(limitStatus);

        ReadingEntity savedReading = readingRepository.save(reading);

        return mapToResponseDTO(savedReading);
    }

    @Transactional
    public void delete(Long id) {
        ReadingEntity reading = findById(id);
        readingRepository.delete(reading);
        log.info("Leitura excluída: ID {}", id);
    }

    private LimitStatusEnum determineLimitStatus(InstrumentEntity instrument, Double value) {
        if (Boolean.TRUE.equals(instrument.getNoLimit())) {
            return LimitStatusEnum.NORMAL;
        }

        StatisticalLimitEntity statisticalLimit = instrument.getStatisticalLimit();
        if (statisticalLimit != null) {
            if (statisticalLimit.getLowerValue() != null && value < statisticalLimit.getLowerValue()) {
                return LimitStatusEnum.INFERIOR;
            }
            if (statisticalLimit.getUpperValue() != null && value > statisticalLimit.getUpperValue()) {
                return LimitStatusEnum.SUPERIOR;
            }
            return LimitStatusEnum.NORMAL;
        }

        DeterministicLimitEntity deterministicLimit = instrument.getDeterministicLimit();
        if (deterministicLimit != null) {
            if (deterministicLimit.getEmergencyValue() != null && value >= deterministicLimit.getEmergencyValue()) {
                return LimitStatusEnum.EMERGENCIA;
            }
            if (deterministicLimit.getAlertValue() != null && value >= deterministicLimit.getAlertValue()) {
                return LimitStatusEnum.ALERTA;
            }
            if (deterministicLimit.getAttentionValue() != null && value >= deterministicLimit.getAttentionValue()) {
                return LimitStatusEnum.ATENCAO;
            }
            return LimitStatusEnum.NORMAL;
        }

        return LimitStatusEnum.NORMAL;
    }

    public ReadingResponseDTO mapToResponseDTO(ReadingEntity reading) {
        ReadingResponseDTO dto = new ReadingResponseDTO();
        dto.setId(reading.getId());
        dto.setDate(reading.getDate());
        dto.setHour(reading.getHour());
        dto.setValue(reading.getValue());
        dto.setLimitStatus(reading.getLimitStatus());
        dto.setInstrumentId(reading.getInstrument().getId());
        dto.setInstrumentName(reading.getInstrument().getName());
        return dto;
    }
}
