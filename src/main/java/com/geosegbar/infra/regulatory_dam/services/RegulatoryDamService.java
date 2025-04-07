package com.geosegbar.infra.regulatory_dam.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.ClassificationDamEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.PotentialDamageEntity;
import com.geosegbar.entities.RegulatoryDamEntity;
import com.geosegbar.entities.RiskCategoryEntity;
import com.geosegbar.entities.SecurityLevelEntity;
import com.geosegbar.entities.SupervisoryBodyEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.classification_dam.peristence.ClassificationDamRepository;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.potential_damage.persistence.PotentialDamageRepository;
import com.geosegbar.infra.regulatory_dam.dtos.RegulatoryDamDTO;
import com.geosegbar.infra.regulatory_dam.persistence.RegulatoryDamRepository;
import com.geosegbar.infra.risk_category.persistence.RiskCategoryRepository;
import com.geosegbar.infra.security_level.persistence.SecurityLevelRepository;
import com.geosegbar.infra.supervisory_body.persistence.SupervisoryBodyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegulatoryDamService {

    private final RegulatoryDamRepository regulatoryDamRepository;
    private final DamRepository damRepository;
    private final SecurityLevelRepository securityLevelRepository;
    private final SupervisoryBodyRepository supervisoryBodyRepository;
    private final RiskCategoryRepository riskCategoryRepository;
    private final PotentialDamageRepository potentialDamageRepository;
    private final ClassificationDamRepository classificationDamRepository;
    
    public RegulatoryDamEntity findById(Long id) {
        return regulatoryDamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Informação regulatória da barragem não encontrada com ID: " + id));
    }
    
    public RegulatoryDamEntity findByDamId(Long damId) {
        return regulatoryDamRepository.findByDamId(damId)
                .orElseThrow(() -> new NotFoundException("Informação regulatória não encontrada para a barragem com ID: " + damId));
    }
    
    public List<RegulatoryDamEntity> findAll() {
        return regulatoryDamRepository.findAll();
    }
    
    @Transactional
    public RegulatoryDamEntity createOrUpdate(RegulatoryDamDTO regulatoryDamDTO) {
        DamEntity dam = damRepository.findById(regulatoryDamDTO.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + regulatoryDamDTO.getDamId()));
        
        RegulatoryDamEntity regulatoryDam;
        
        if (regulatoryDamDTO.getId() != null) {
            regulatoryDam = regulatoryDamRepository.findById(regulatoryDamDTO.getId())
                    .orElseThrow(() -> new NotFoundException("Informação regulatória da barragem não encontrada com ID: " + regulatoryDamDTO.getId()));
            
            // Ensure we're not trying to change dam association
            if (!regulatoryDam.getDam().getId().equals(regulatoryDamDTO.getDamId())) {
                throw new IllegalArgumentException("Não é permitido mudar a barragem associada à informação regulatória");
            }
        } else {
            // Check if regulatory data already exists for this dam
            if (regulatoryDamRepository.existsByDam(dam)) {
                throw new DuplicateResourceException("Já existe informação regulatória para esta barragem");
            }
            
            regulatoryDam = new RegulatoryDamEntity();
            regulatoryDam.setDam(dam);
            dam.setRegulatoryDam(regulatoryDam); // Set the bidirectional relationship
        }
        
        // Update all fields
        regulatoryDam.setFramePNSB(regulatoryDamDTO.getFramePNSB());
        regulatoryDam.setRepresentativeName(regulatoryDamDTO.getRepresentativeName());
        regulatoryDam.setRepresentativeEmail(regulatoryDamDTO.getRepresentativeEmail());
        regulatoryDam.setRepresentativePhone(regulatoryDamDTO.getRepresentativePhone());
        regulatoryDam.setTechnicalManagerName(regulatoryDamDTO.getTechnicalManagerName());
        regulatoryDam.setTechnicalManagerEmail(regulatoryDamDTO.getTechnicalManagerEmail());
        regulatoryDam.setTechnicalManagerPhone(regulatoryDamDTO.getTechnicalManagerPhone());
        
        // Set relationships
        if (regulatoryDamDTO.getSecurityLevelId() != null) {
            SecurityLevelEntity securityLevel = securityLevelRepository.findById(regulatoryDamDTO.getSecurityLevelId())
                    .orElseThrow(() -> new NotFoundException("Nível de segurança não encontrado com ID: " + regulatoryDamDTO.getSecurityLevelId()));
            regulatoryDam.setSecurityLevel(securityLevel);
        } else {
            regulatoryDam.setSecurityLevel(null);
        }
        
        if (regulatoryDamDTO.getSupervisoryBodyId() != null) {
            SupervisoryBodyEntity supervisoryBody = supervisoryBodyRepository.findById(regulatoryDamDTO.getSupervisoryBodyId())
                    .orElseThrow(() -> new NotFoundException("Órgão fiscalizador não encontrado com ID: " + regulatoryDamDTO.getSupervisoryBodyId()));
            regulatoryDam.setSupervisoryBody(supervisoryBody);
        } else {
            regulatoryDam.setSupervisoryBody(null);
        }
        
        if (regulatoryDamDTO.getRiskCategoryId() != null) {
            RiskCategoryEntity riskCategory = riskCategoryRepository.findById(regulatoryDamDTO.getRiskCategoryId())
                    .orElseThrow(() -> new NotFoundException("Categoria de risco não encontrada com ID: " + regulatoryDamDTO.getRiskCategoryId()));
            regulatoryDam.setRiskCategory(riskCategory);
        } else {
            regulatoryDam.setRiskCategory(null);
        }
        
        if (regulatoryDamDTO.getPotentialDamageId() != null) {
            PotentialDamageEntity potentialDamage = potentialDamageRepository.findById(regulatoryDamDTO.getPotentialDamageId())
                    .orElseThrow(() -> new NotFoundException("Dano potencial não encontrado com ID: " + regulatoryDamDTO.getPotentialDamageId()));
            regulatoryDam.setPotentialDamage(potentialDamage);
        } else {
            regulatoryDam.setPotentialDamage(null);
        }
        
        if (regulatoryDamDTO.getClassificationDamId() != null) {
            ClassificationDamEntity classificationDam = classificationDamRepository.findById(regulatoryDamDTO.getClassificationDamId())
                    .orElseThrow(() -> new NotFoundException("Classificação de barragem não encontrada com ID: " + regulatoryDamDTO.getClassificationDamId()));
            regulatoryDam.setClassificationDam(classificationDam);
        } else {
            regulatoryDam.setClassificationDam(null);
        }
        
        RegulatoryDamEntity savedRegulatoryDam = regulatoryDamRepository.save(regulatoryDam);
        log.info("Informação regulatória {} para a barragem {}", 
                regulatoryDamDTO.getId() == null ? "criada" : "atualizada", 
                dam.getName());
        
        return savedRegulatoryDam;
    }
    
    @Transactional
    public void delete(Long id) {
        RegulatoryDamEntity regulatoryDam = regulatoryDamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Informação regulatória da barragem não encontrada com ID: " + id));
        
        // Handle bidirectional relationship
        DamEntity dam = regulatoryDam.getDam();
        if (dam != null) {
            dam.setRegulatoryDam(null);
            damRepository.save(dam);
        }
        
        regulatoryDamRepository.delete(regulatoryDam);
        log.info("Informação regulatória excluída para a barragem {}", 
                dam != null ? dam.getName() : "desconhecida");
    }
}