package com.geosegbar.infra.sex.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.SexEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.sex.persistence.jpa.SexRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SexService {

    private final SexRepository sexRepository;

    @PostConstruct
    @Transactional
    public void initializeDefaultSexes() {
        createIfNotExists("Feminino");
        createIfNotExists("Masculino");
    }

    private void createIfNotExists(String name) {
        if (!sexRepository.existsByName(name)) {
            SexEntity sex = new SexEntity();
            sex.setName(name);
            sexRepository.save(sex);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        if (!sexRepository.existsById(id)) {
            throw new NotFoundException("Sexo não encontrado para exclusão!");
        }
        sexRepository.deleteById(id);
    }

    @Transactional
    public SexEntity save(SexEntity sexEntity) {
        if (sexRepository.existsByName(sexEntity.getName())) {
            throw new DuplicateResourceException("Já existe um sexo com este nome!");
        }
        return sexRepository.save(sexEntity);
    }

    @Transactional
    public SexEntity update(SexEntity sexEntity) {
        if (!sexRepository.existsById(sexEntity.getId())) {
            throw new NotFoundException("Sexo não encontrado para atualização!");
        }

        if (sexRepository.existsByNameAndIdNot(sexEntity.getName(), sexEntity.getId())) {
            throw new DuplicateResourceException("Já existe um sexo com este nome!");
        }

        return sexRepository.save(sexEntity);
    }

    @Transactional(readOnly = true)
    public SexEntity findById(Long id) {
        return sexRepository.findById(id).
                orElseThrow(() -> new NotFoundException("Sexo não encontrado!"));
    }

    @Transactional(readOnly = true)
    public List<SexEntity> findAll() {
        return sexRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return sexRepository.existsByName(name);
    }

    @Transactional(readOnly = true)
    public boolean existsByNameAndIdNot(String name, Long id) {
        return sexRepository.existsByNameAndIdNot(name, id);
    }
}
