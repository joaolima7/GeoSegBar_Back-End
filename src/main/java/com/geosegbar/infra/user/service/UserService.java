package com.geosegbar.infra.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;

    @Transactional
    public void deleteById(Long id) {
        userRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Usuário não encontrado para exclusão!"));

        userRepository.deleteById(id);
    }

    @Transactional
    public UserEntity save(UserEntity userEntity) {
        if(userRepository.existsByEmail(userEntity.getEmail())){
            throw new DuplicateResourceException("Já existe um usuário com o email informado!");
        }
        
        return userRepository.save(userEntity);
    }

    @Transactional
    public UserEntity update(UserEntity userEntity) {
        userRepository.findById(userEntity.getId()).
        orElseThrow(() -> new NotFoundException("Usuário não encontrado para atualização!"));

        if(userRepository.existsByEmailAndIdNot(userEntity.getEmail(), userEntity.getId())) {
            throw new DuplicateResourceException("Já existe um usuário com o email informado!");
        }

        return userRepository.save(userEntity);
    }

    public UserEntity findById(Long id) {
        return userRepository.findById(id).
        orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
    }

    public List<UserEntity> findAll() {
        return userRepository.findAllByOrderByIdAsc();
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByEmailAndIdNot(String email, Long id) {
        return userRepository.existsByEmailAndIdNot(email, id);
    }
}
