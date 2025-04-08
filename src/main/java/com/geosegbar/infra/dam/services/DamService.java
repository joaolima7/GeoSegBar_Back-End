package com.geosegbar.infra.dam.services;

import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;

import com.geosegbar.entities.ClassificationDamEntity;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DocumentationDamEntity;
import com.geosegbar.entities.PotentialDamageEntity;
import com.geosegbar.entities.RegulatoryDamEntity;
import com.geosegbar.entities.RiskCategoryEntity;
import com.geosegbar.entities.SecurityLevelEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.entities.SupervisoryBodyEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.classification_dam.peristence.ClassificationDamRepository;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.dam.dtos.CreateDamCompleteRequest;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.documentation_dam.persistence.DocumentationDamRepository;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.potential_damage.persistence.PotentialDamageRepository;
import com.geosegbar.infra.regulatory_dam.persistence.RegulatoryDamRepository;
import com.geosegbar.infra.risk_category.persistence.RiskCategoryRepository;
import com.geosegbar.infra.security_level.persistence.SecurityLevelRepository;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;
import com.geosegbar.infra.supervisory_body.persistence.SupervisoryBodyRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DamService {
    

    private final DamRepository damRepository;
    private final ClientRepository clientRepository;
    private final StatusRepository statusRepository;
    private final SecurityLevelRepository securityLevelRepository;
    private final SupervisoryBodyRepository supervisoryBodyRepository;
    private final RiskCategoryRepository riskCategoryRepository;
    private final PotentialDamageRepository potentialDamageRepository;
    private final ClassificationDamRepository classificationDamRepository;
    private final DocumentationDamRepository documentationDamRepository;
    private final RegulatoryDamRepository regulatoryDamRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public DamEntity createCompleteWithRelationships(CreateDamCompleteRequest request) {
        // Verificar se já existe uma barragem com esse nome
        if (damRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Já existe uma barragem com este nome!");
        }
        
        // Buscar entidades relacionadas
        ClientEntity client = clientRepository.findById(request.getClientId())
            .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
            
        StatusEntity status = statusRepository.findById(request.getStatusId())
            .orElseThrow(() -> new NotFoundException("Status não encontrado"));
            
        // Criar entidade Dam
        DamEntity dam = new DamEntity();
        dam.setName(request.getName());
        dam.setLatitude(request.getLatitude());
        dam.setLongitude(request.getLongitude());
        dam.setStreet(request.getStreet());
        dam.setNeighborhood(request.getNeighborhood());
        dam.setNumberAddress(request.getNumberAddress());
        dam.setCity(request.getCity());
        dam.setState(request.getState());
        dam.setZipCode(request.getZipCode());
        dam.setClient(client);
        dam.setStatus(status);
        dam.setLinkPSB(request.getLinkPSB());
        dam.setLinkLegislation(request.getLinkLegislation());
        
        // Processar imagens
        if (request.getLogoBase64() != null && !request.getLogoBase64().isEmpty()) {
            // Remover prefixo da string base64 se existir
            String base64Image = request.getLogoBase64();
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            String logoUrl = fileStorageService.storeFileFromBytes(
                imageBytes, 
                "logo.jpg", 
                "image/jpeg", 
                "logos"
            );
            dam.setLogoPath(logoUrl);
        }
        
        if (request.getDamImageBase64() != null && !request.getDamImageBase64().isEmpty()) {
            // Remover prefixo da string base64 se existir
            String base64Image = request.getDamImageBase64();
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }
            
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            String damImageUrl = fileStorageService.storeFileFromBytes(
                imageBytes, 
                "dam_image.jpg", 
                "image/jpeg", 
                "dam_images"
            );
            dam.setDamImagePath(damImageUrl);
        }
        
        // Salvar Dam primeiro para ter o ID
        dam = damRepository.save(dam);
        
        // Criar DocumentationDam
        DocumentationDamEntity documentationDam = new DocumentationDamEntity();
        documentationDam.setDam(dam);
        documentationDam.setLastUpdatePAE(request.getLastUpdatePAE());
        documentationDam.setNextUpdatePAE(request.getNextUpdatePAE());
        documentationDam.setLastUpdatePSB(request.getLastUpdatePSB());
        documentationDam.setNextUpdatePSB(request.getNextUpdatePSB());
        documentationDam.setLastUpdateRPSB(request.getLastUpdateRPSB());
        documentationDam.setNextUpdateRPSB(request.getNextUpdateRPSB());
        documentationDam.setLastAchievementISR(request.getLastAchievementISR());
        documentationDam.setNextAchievementISR(request.getNextAchievementISR());
        documentationDam.setLastAchievementChecklist(request.getLastAchievementChecklist());
        documentationDam.setNextAchievementChecklist(request.getNextAchievementChecklist());
        documentationDam.setLastFillingFSB(request.getLastFillingFSB());
        documentationDam.setNextFillingFSB(request.getNextFillingFSB());
        
        // Salvar DocumentationDam
        documentationDam = documentationDamRepository.save(documentationDam);
        
        // Criar RegulatoryDam
        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setDam(dam);
        regulatoryDam.setFramePNSB(request.getFramePNSB());
        regulatoryDam.setRepresentativeName(request.getRepresentativeName());
        regulatoryDam.setRepresentativeEmail(request.getRepresentativeEmail());
        regulatoryDam.setRepresentativePhone(request.getRepresentativePhone());
        regulatoryDam.setTechnicalManagerName(request.getTechnicalManagerName());
        regulatoryDam.setTechnicalManagerEmail(request.getTechnicalManagerEmail());
        regulatoryDam.setTechnicalManagerPhone(request.getTechnicalManagerPhone());
        
        // Associar entidades relacionadas a RegulatoryDam
        if (request.getSecurityLevelId() != null) {
            SecurityLevelEntity securityLevel = securityLevelRepository.findById(request.getSecurityLevelId())
                .orElseThrow(() -> new NotFoundException("Nível de segurança não encontrado"));
            regulatoryDam.setSecurityLevel(securityLevel);
        }
        
        if (request.getSupervisoryBodyId() != null) {
            SupervisoryBodyEntity supervisoryBody = supervisoryBodyRepository.findById(request.getSupervisoryBodyId())
                .orElseThrow(() -> new NotFoundException("Órgão fiscalizador não encontrado"));
            regulatoryDam.setSupervisoryBody(supervisoryBody);
        }
        
        if (request.getRiskCategoryId() != null) {
            RiskCategoryEntity riskCategory = riskCategoryRepository.findById(request.getRiskCategoryId())
                .orElseThrow(() -> new NotFoundException("Categoria de risco não encontrada"));
            regulatoryDam.setRiskCategory(riskCategory);
        }
        
        if (request.getPotentialDamageId() != null) {
            PotentialDamageEntity potentialDamage = potentialDamageRepository.findById(request.getPotentialDamageId())
                .orElseThrow(() -> new NotFoundException("Dano potencial não encontrado"));
            regulatoryDam.setPotentialDamage(potentialDamage);
        }
        
        if (request.getClassificationDamId() != null) {
            ClassificationDamEntity classificationDam = classificationDamRepository.findById(request.getClassificationDamId())
                .orElseThrow(() -> new NotFoundException("Classificação da barragem não encontrada"));
            regulatoryDam.setClassificationDam(classificationDam);
        }
        
        // Salvar RegulatoryDam
        regulatoryDam = regulatoryDamRepository.save(regulatoryDam);
        
        // Recarregar Dam com todos os relacionamentos
        return findById(dam.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        damRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Barragem não encontrada para exclusão!"));

        damRepository.deleteById(id);
    }

    @Transactional
    public DamEntity save(DamEntity damEntity) {
        if (damRepository.existsByName(damEntity.getName())) {
            throw new DuplicateResourceException("Já existe uma barragem com este nome!");
        }

        return damRepository.save(damEntity);
    }

    @Transactional
    public DamEntity update(DamEntity damEntity) {
        damRepository.findById(damEntity.getId()).
        orElseThrow(() -> new NotFoundException("Endereço não encontrado para atualização!"));

        if (damRepository.existsByNameAndIdNot(damEntity.getName(), damEntity.getId())) {
            throw new DuplicateResourceException("Já existe uma barragem com este nome!");
        }

        return damRepository.save(damEntity);
    }

    public DamEntity findById(Long id) {
        return damRepository.findById(id).
        orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));
    }

    public List<DamEntity> findAll() {
        return damRepository.findAllByOrderByIdAsc();
    }

    public boolean existsByName(String name) {
        return damRepository.existsByName(name);
    }

    public boolean existsByNameAndIdNot(String name, Long id) {
        return damRepository.existsByNameAndIdNot(name, id);
    }
}
