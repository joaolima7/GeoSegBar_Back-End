package com.geosegbar.infra.option.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.OptionEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.option.persistence.jpa.OptionRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OptionService {

    private final OptionRepository optionRepository;

    @PostConstruct
    @Transactional
    public void initializeDefaultOptions() {

        createIfNotExists("NE", "Não Existe", 1);
        createIfNotExists("PV", "Primeira Vez", 2);
        createIfNotExists("AU", "Aumentou", 3);
        createIfNotExists("DM", "Diminuiu", 4);
        createIfNotExists("DS", "Desapareceu", 5);
        createIfNotExists("PC", "Permaneceu Constante", 6);
        createIfNotExists("NI", "Não Inspecionado", 7);

    }

    private void createIfNotExists(String label, String value, Integer orderIndex) {
        Optional<OptionEntity> existingOption = optionRepository.findByLabel(label);

        if (existingOption.isEmpty()) {
            OptionEntity option = new OptionEntity();
            option.setLabel(label);
            option.setValue(value);
            option.setOrderIndex(orderIndex);
            optionRepository.save(option);
        }
    }

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
