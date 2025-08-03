package com.geosegbar.infra.hydrotelemetric.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.geosegbar.entities.HydrotelemetricReadingEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.hydrotelemetric.persistence.jpa.HydrotelemetricReadingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HydrotelemetricReadingService {

    private final HydrotelemetricReadingRepository hydrotelemetricReadingRepository;

    public List<HydrotelemetricReadingEntity> getAllReadings() {
        return hydrotelemetricReadingRepository.findAll(Sort.by(Sort.Direction.DESC, "date"));
    }

    public Optional<Double> getLatestUpstreamAverageByDamId(Long damId) {
        return hydrotelemetricReadingRepository.findLatestUpstreamAverageByDamId(damId);
    }

    public Page<HydrotelemetricReadingEntity> getAllReadingsPaginated(int page, int size) {
        return hydrotelemetricReadingRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date")));
    }

    public HydrotelemetricReadingEntity getReadingById(Long id) {
        return hydrotelemetricReadingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leitura hidrotelemétrica não encontrada com ID: " + id));
    }

    public List<HydrotelemetricReadingEntity> getReadingsByDamId(Long damId) {
        return hydrotelemetricReadingRepository.findByDamIdOrderByDateDesc(damId);
    }

    public List<HydrotelemetricReadingEntity> getReadingsByDamIdOrderedByDateDesc(Long damId) {
        return hydrotelemetricReadingRepository.findByDamIdOrderByDateDesc(damId);
    }

    public List<HydrotelemetricReadingEntity> getReadingsByDamIdAndDateRange(Long damId, LocalDate startDate, LocalDate endDate) {
        return hydrotelemetricReadingRepository.findByDamIdAndDateBetweenOrderByDateDesc(damId, startDate, endDate);
    }

    public List<HydrotelemetricReadingEntity> getReadingsByDateRange(LocalDate startDate, LocalDate endDate) {
        return hydrotelemetricReadingRepository.findByDateBetweenOrderByDateDesc(startDate, endDate);
    }

    public Page<HydrotelemetricReadingEntity> getFilteredReadingsPaginated(Long damId, LocalDate startDate,
            LocalDate endDate, String sortOrder,
            int page, int size) {
        Sort sort = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.by(Sort.Direction.ASC, "date")
                : Sort.by(Sort.Direction.DESC, "date");

        Pageable pageable = PageRequest.of(page, size, sort);

        if (damId != null) {
            if (startDate != null && endDate != null) {
                return hydrotelemetricReadingRepository.findByDamIdAndDateBetween(damId, startDate, endDate, pageable);
            } else {
                return hydrotelemetricReadingRepository.findByDamId(damId, pageable);
            }
        } else if (startDate != null && endDate != null) {
            return hydrotelemetricReadingRepository.findByDateBetween(startDate, endDate, pageable);
        } else {
            return hydrotelemetricReadingRepository.findAll(pageable);
        }
    }

    public List<HydrotelemetricReadingEntity> getFilteredReadings(Long damId, LocalDate startDate, LocalDate endDate, String sortOrder) {
        boolean sortAscending = "asc".equalsIgnoreCase(sortOrder);

        if (damId != null) {
            if (startDate != null && endDate != null) {
                if (sortAscending) {
                    return hydrotelemetricReadingRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);
                } else {
                    return hydrotelemetricReadingRepository.findByDamIdAndDateBetweenOrderByDateDesc(damId, startDate, endDate);
                }
            } else {
                return hydrotelemetricReadingRepository.findByDamIdOrderByDateDesc(damId);
            }
        } else if (startDate != null && endDate != null) {
            if (sortAscending) {
                return hydrotelemetricReadingRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);
            } else {
                return hydrotelemetricReadingRepository.findByDateBetweenOrderByDateDesc(startDate, endDate);
            }
        } else {
            return getAllReadings();
        }
    }
}
