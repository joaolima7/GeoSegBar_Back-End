package com.geosegbar.infra.reservoir.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.LevelEntity;
import com.geosegbar.entities.ReservoirEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.level.persistence.LevelRepository;
import com.geosegbar.infra.reservoir.persistence.ReservoirRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservoirService {
    
    private final ReservoirRepository reservoirRepository;
    private final DamRepository damRepository;
    private final LevelRepository levelRepository;
    
    public List<ReservoirEntity> findByDamId(Long damId) {
        return reservoirRepository.findByDamIdOrderByCreatedAtDesc(damId);
    }
    
    public ReservoirEntity findById(Long id) {
        return reservoirRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservatório não encontrado!"));
    }
    
    @Transactional
    public ReservoirEntity save(ReservoirEntity reservoir, Long damId, Long levelId) {
        DamEntity dam = damRepository.findById(damId)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));
                
        LevelEntity level = levelRepository.findById(levelId)
                .orElseThrow(() -> new NotFoundException("Nível não encontrado!"));
                
        reservoir.setDam(dam);
        reservoir.setLevel(level);
        
        return reservoirRepository.save(reservoir);
    }
    
    @Transactional
    public ReservoirEntity update(ReservoirEntity reservoir, Long id, Long levelId) {
        ReservoirEntity existingReservoir = reservoirRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservatório não encontrado para atualização!"));
                
        LevelEntity level = levelRepository.findById(levelId)
                .orElseThrow(() -> new NotFoundException("Nível não encontrado!"));
                
        existingReservoir.setLevel(level);
        
        return reservoirRepository.save(existingReservoir);
    }
    
    @Transactional
    public void deleteById(Long id) {
        reservoirRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservatório não encontrado para exclusão!"));
        reservoirRepository.deleteById(id);
    }
}