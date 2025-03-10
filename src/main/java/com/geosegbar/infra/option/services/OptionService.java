package com.geosegbar.infra.option.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.OptionEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OptionService {

    private final OptionRepository optionRepository;

    @Transactional
    public void deleteById(Long id) {
        optionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Opção não encontrada para exclusão!"));
        optionRepository.deleteById(id);
    }

    @Transactional
    public OptionEntity save(OptionEntity option) {
        return optionRepository.save(option);
    }

    @Transactional
    public OptionEntity update(OptionEntity option) {
        optionRepository.findById(option.getId())
            .orElseThrow(() -> new NotFoundException("Opção não encontrada para atualização!"));
        return optionRepository.save(option);
    }

    public OptionEntity findById(Long id) {
        return optionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Opção não encontrada!"));
    }

    public List<OptionEntity> findAll() {
        return optionRepository.findAllByOrderByOrderIndexAsc();
    }
}
