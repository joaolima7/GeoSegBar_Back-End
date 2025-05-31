package com.geosegbar.infra.security_level.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.SecurityLevelEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.security_level.persistence.SecurityLevelRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityLevelService {

    private final SecurityLevelRepository securityLevelRepository;

    @PostConstruct
    @Transactional
    public void initializeDefaultSecurityLevels() {
        createIfNotExists("Normal");
        createIfNotExists("Atenção");
        createIfNotExists("Alerta");
        createIfNotExists("Emergência");
    }

    private void createIfNotExists(String level) {
        if (!securityLevelRepository.existsByLevel(level)) {
            SecurityLevelEntity securityLevel = new SecurityLevelEntity();
            securityLevel.setLevel(level);
            securityLevelRepository.save(securityLevel);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        securityLevelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nível de segurança não encontrado para exclusão!"));

        securityLevelRepository.deleteById(id);
    }

    @Transactional
    public SecurityLevelEntity save(SecurityLevelEntity securityLevelEntity) {
        if (securityLevelRepository.existsByLevel(securityLevelEntity.getLevel())) {
            throw new DuplicateResourceException("Já existe um nível de segurança com este nome!");
        }

        return securityLevelRepository.save(securityLevelEntity);
    }

    @Transactional
    public SecurityLevelEntity update(SecurityLevelEntity securityLevelEntity) {
        securityLevelRepository.findById(securityLevelEntity.getId())
                .orElseThrow(() -> new NotFoundException("Nível de segurança não encontrado para atualização!"));

        if (securityLevelRepository.existsByLevelAndIdNot(securityLevelEntity.getLevel(), securityLevelEntity.getId())) {
            throw new DuplicateResourceException("Já existe um nível de segurança com este nome!");
        }

        return securityLevelRepository.save(securityLevelEntity);
    }

    public SecurityLevelEntity findById(Long id) {
        return securityLevelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nível de segurança não encontrado!"));
    }

    public List<SecurityLevelEntity> findAll() {
        return securityLevelRepository.findAllByOrderByIdAsc();
    }
}
