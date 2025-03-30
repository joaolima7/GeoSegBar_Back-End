package com.geosegbar.infra.roles.initializer;

import org.springframework.stereotype.Component;

import com.geosegbar.common.enums.RoleEnum;
import com.geosegbar.entities.RoleEntity;
import com.geosegbar.infra.roles.persistence.RoleRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer {
    
    private final RoleRepository roleRepository;
    
    @PostConstruct
    public void init() {
        log.info("Initializing roles...");
        
        if (!roleRepository.existsByName(RoleEnum.ADMIN)) {
            roleRepository.save(new RoleEntity(RoleEnum.ADMIN, "Administrador: possui acesso total ao sistema."));
            log.info("ADMIN role created");
        }
        
        if (!roleRepository.existsByName(RoleEnum.COLLABORATOR)) {
            roleRepository.save(new RoleEntity(RoleEnum.COLLABORATOR, "Colaborador: possui acesso limitado ao sistema."));
            log.info("COLLABORATOR role created");
        }
        
        log.info("Roles initialization completed");
    }
}