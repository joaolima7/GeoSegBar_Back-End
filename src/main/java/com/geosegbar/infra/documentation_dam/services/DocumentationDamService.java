package com.geosegbar.infra.documentation_dam.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DocumentationDamEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.documentation_dam.dtos.DocumentationDamDTO;
import com.geosegbar.infra.documentation_dam.persistence.DocumentationDamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentationDamService {

    private final DocumentationDamRepository documentationDamRepository;
    private final DamRepository damRepository;

    public DocumentationDamEntity findById(Long id) {
        return documentationDamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Documentação de barragem não encontrada com ID: " + id));
    }

    public DocumentationDamEntity findByDamId(Long damId) {
        return documentationDamRepository.findByDamId(damId)
                .orElseThrow(() -> new NotFoundException("Documentação não encontrada para a barragem com ID: " + damId));
    }

    public List<DocumentationDamEntity> findAll() {
        return documentationDamRepository.findAll();
    }

    @Transactional
    public DocumentationDamEntity createOrUpdate(DocumentationDamDTO documentationDamDTO) {
        DamEntity dam = damRepository.findById(documentationDamDTO.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + documentationDamDTO.getDamId()));

        DocumentationDamEntity documentationDam;

        if (documentationDamDTO.getId() != null) {
            documentationDam = documentationDamRepository.findById(documentationDamDTO.getId())
                    .orElseThrow(() -> new NotFoundException("Documentação de barragem não encontrada com ID: " + documentationDamDTO.getId()));

            if (!documentationDam.getDam().getId().equals(documentationDamDTO.getDamId())) {
                throw new DuplicateResourceException("Não é permitido mudar a barragem associada à documentação");
            }
        } else {
            if (documentationDamRepository.existsByDam(dam)) {
                throw new DuplicateResourceException("Já existe documentação para esta barragem");
            }

            documentationDam = new DocumentationDamEntity();
            documentationDam.setDam(dam);
            dam.setDocumentationDam(documentationDam);
        }

        // Update all fields
        documentationDam.setLastUpdatePAE(documentationDamDTO.getLastUpdatePAE());
        documentationDam.setNextUpdatePAE(documentationDamDTO.getNextUpdatePAE());
        documentationDam.setLastUpdatePSB(documentationDamDTO.getLastUpdatePSB());
        documentationDam.setNextUpdatePSB(documentationDamDTO.getNextUpdatePSB());
        documentationDam.setLastUpdateRPSB(documentationDamDTO.getLastUpdateRPSB());
        documentationDam.setNextUpdateRPSB(documentationDamDTO.getNextUpdateRPSB());
        documentationDam.setLastAchievementISR(documentationDamDTO.getLastAchievementISR());
        documentationDam.setNextAchievementISR(documentationDamDTO.getNextAchievementISR());
        documentationDam.setLastAchievementChecklist(documentationDamDTO.getLastAchievementChecklist());
        documentationDam.setNextAchievementChecklist(documentationDamDTO.getNextAchievementChecklist());
        documentationDam.setLastFillingFSB(documentationDamDTO.getLastFillingFSB());
        documentationDam.setNextFillingFSB(documentationDamDTO.getNextFillingFSB());
        documentationDam.setLastInternalSimulation(documentationDamDTO.getLastInternalSimulation());
        documentationDam.setNextInternalSimulation(documentationDamDTO.getNextInternalSimulation());
        documentationDam.setLastExternalSimulation(documentationDamDTO.getLastExternalSimulation());
        documentationDam.setNextExternalSimulation(documentationDamDTO.getNextExternalSimulation());

        return documentationDamRepository.save(documentationDam);
    }

    @Transactional
    public void delete(Long id) {
        DocumentationDamEntity documentationDam = documentationDamRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Documentação de barragem não encontrada com ID: " + id));

        DamEntity dam = documentationDam.getDam();
        if (dam != null) {
            dam.setDocumentationDam(null);
            damRepository.save(dam);
        }

        documentationDamRepository.delete(documentationDam);
    }
}
