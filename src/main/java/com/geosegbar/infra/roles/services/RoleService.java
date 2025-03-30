package com.geosegbar.infra.roles.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.RoleEntity;
import com.geosegbar.infra.roles.persistence.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    
    public List<RoleEntity> findAll() {
        return roleRepository.findAll();
    }
}
