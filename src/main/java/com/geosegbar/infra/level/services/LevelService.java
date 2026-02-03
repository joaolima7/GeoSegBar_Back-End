package com.geosegbar.infra.level.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.LevelEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.level.persistence.LevelRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LevelService {

    private final LevelRepository levelRepository;

    @Transactional(readOnly = true)
    public List<LevelEntity> findAll() {
        return levelRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public LevelEntity findById(Long id) {
        return levelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nível não encontrado!"));
    }

    @Transactional
    public LevelEntity save(LevelEntity level) {
        if (levelRepository.existsByName(level.getName())) {
            throw new DuplicateResourceException("Já existe um nível com este nome!");
        }
        return levelRepository.save(level);
    }

    @Transactional
    public LevelEntity update(LevelEntity level) {

        if (!levelRepository.existsById(level.getId())) {
            throw new NotFoundException("Nível não encontrado para atualização!");
        }

        if (levelRepository.existsByNameAndIdNot(level.getName(), level.getId())) {
            throw new DuplicateResourceException("Já existe um nível com este nome!");
        }

        return levelRepository.save(level);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!levelRepository.existsById(id)) {
            throw new NotFoundException("Nível não encontrado para exclusão!");
        }
        levelRepository.deleteById(id);
    }
}
