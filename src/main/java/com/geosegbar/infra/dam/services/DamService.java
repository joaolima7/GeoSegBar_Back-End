package com.geosegbar.infra.dam.services;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.ClassificationDamEntity;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DocumentationDamEntity;
import com.geosegbar.entities.LevelEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.PotentialDamageEntity;
import com.geosegbar.entities.RegulatoryDamEntity;
import com.geosegbar.entities.ReservoirEntity;
import com.geosegbar.entities.RiskCategoryEntity;
import com.geosegbar.entities.SecurityLevelEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.infra.classification_dam.peristence.ClassificationDamRepository;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.dam.dtos.CreateDamCompleteRequest;
import com.geosegbar.infra.dam.dtos.LevelRequestDTO;
import com.geosegbar.infra.dam.dtos.ReservoirRequestDTO;
import com.geosegbar.infra.dam.dtos.UpdateDamRequest;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.documentation_dam.persistence.DocumentationDamRepository;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.level.persistence.LevelRepository;
import com.geosegbar.infra.potential_damage.persistence.PotentialDamageRepository;
import com.geosegbar.infra.psb.services.PSBFolderService;
import com.geosegbar.infra.regulatory_dam.persistence.RegulatoryDamRepository;
import com.geosegbar.infra.reservoir.persistence.ReservoirRepository;
import com.geosegbar.infra.risk_category.persistence.RiskCategoryRepository;
import com.geosegbar.infra.security_level.persistence.SecurityLevelRepository;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DamService {

    private final DamRepository damRepository;
    private final ClientRepository clientRepository;
    private final StatusRepository statusRepository;
    private final SecurityLevelRepository securityLevelRepository;
    private final RiskCategoryRepository riskCategoryRepository;
    private final PotentialDamageRepository potentialDamageRepository;
    private final ClassificationDamRepository classificationDamRepository;
    private final DocumentationDamRepository documentationDamRepository;
    private final RegulatoryDamRepository regulatoryDamRepository;
    private final FileStorageService fileStorageService;
    private final LevelRepository levelRepository;
    private final ReservoirRepository reservoirRepository;
    private final PSBFolderService psbFolderService;

    public DamEntity findByIdWithSections(Long id) {
        DamEntity dam = findById(id);
        Hibernate.initialize(dam.getSections());
        return dam;
    }

    public List<DamEntity> findAllWithSections() {
        List<DamEntity> dams = findAll();
        dams.forEach(dam -> Hibernate.initialize(dam.getSections()));
        return dams;
    }

    public List<DamEntity> findDamsByClientIdWithSections(Long clientId) {
        List<DamEntity> dams = findDamsByClientId(clientId);
        dams.forEach(dam -> Hibernate.initialize(dam.getSections()));
        return dams;
    }

    public List<DamEntity> findByClientAndStatusWithSections(Long clientId, Long statusId) {
        List<DamEntity> dams = findByClientAndStatus(clientId, statusId);
        dams.forEach(dam -> Hibernate.initialize(dam.getSections()));
        return dams;
    }

    @Transactional
    public DamEntity createCompleteWithRelationships(CreateDamCompleteRequest request) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getAttributionsPermission().getEditDam()) {
                throw new UnauthorizedException("Usuário não tem permissão para criar barragens!");
            }
        }

        if (damRepository.existsByNameAndClientId(request.getName(), request.getClientId())) {
            throw new DuplicateResourceException("Já existe uma barragem com este nome para este cliente!");
        }

        ClientEntity client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));

        StatusEntity status = statusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new NotFoundException("Status não encontrado"));

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

        if (request.getLogoBase64() != null && !request.getLogoBase64().isEmpty()) {
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

        dam = damRepository.save(dam);

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
        documentationDam.setLastInternalSimulation(request.getLastInternalSimulation());
        documentationDam.setNextInternalSimulation(request.getNextInternalSimulation());
        documentationDam.setLastExternalSimulation(request.getLastExternalSimulation());
        documentationDam.setNextExternalSimulation(request.getNextExternalSimulation());

        documentationDamRepository.save(documentationDam);

        RegulatoryDamEntity regulatoryDam = new RegulatoryDamEntity();
        regulatoryDam.setDam(dam);
        regulatoryDam.setFramePNSB(request.getFramePNSB());
        regulatoryDam.setRepresentativeName(request.getRepresentativeName());
        regulatoryDam.setRepresentativeEmail(request.getRepresentativeEmail());
        regulatoryDam.setRepresentativePhone(request.getRepresentativePhone());
        regulatoryDam.setTechnicalManagerName(request.getTechnicalManagerName());
        regulatoryDam.setTechnicalManagerEmail(request.getTechnicalManagerEmail());
        regulatoryDam.setTechnicalManagerPhone(request.getTechnicalManagerPhone());

        if (request.getSecurityLevelId() != null) {
            SecurityLevelEntity securityLevel = securityLevelRepository.findById(request.getSecurityLevelId())
                    .orElseThrow(() -> new NotFoundException("Nível de segurança não encontrado"));
            regulatoryDam.setSecurityLevel(securityLevel);
        }

        if (request.getSupervisoryBodyName() != null) {
            regulatoryDam.setSupervisoryBodyName(request.getSupervisoryBodyName());

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

        regulatoryDamRepository.save(regulatoryDam);

        if (request.getReservoirs() != null && !request.getReservoirs().isEmpty()) {
            for (ReservoirRequestDTO reservoirDTO : request.getReservoirs()) {
                LevelEntity level = processLevel(reservoirDTO.getLevel());

                ReservoirEntity reservoir = new ReservoirEntity();
                reservoir.setDam(dam);
                reservoir.setLevel(level);

                reservoirRepository.save(reservoir);
            }
        }

        if (request.getPsbFolders() != null && !request.getPsbFolders().isEmpty()) {
            List<PSBFolderEntity> foldersList = psbFolderService.createMultipleFolders(
                    dam,
                    request.getPsbFolders(),
                    request.getCreatedById()
            );

            Set<PSBFolderEntity> foldersSet = new HashSet<>(foldersList);
            dam.setPsbFolders(foldersSet);
        }

        return findById(dam.getId());
    }

    private LevelEntity processLevel(LevelRequestDTO levelDTO) {
        if (levelDTO.getId() != null) {
            return levelRepository.findById(levelDTO.getId())
                    .orElseThrow(() -> new NotFoundException("Nível não encontrado com ID: " + levelDTO.getId()));
        }

        return levelRepository.findByName(levelDTO.getName())
                .orElseGet(() -> {
                    LevelEntity newLevel = new LevelEntity();
                    newLevel.setName(levelDTO.getName());
                    newLevel.setValue(levelDTO.getValue());
                    newLevel.setUnitLevel(levelDTO.getUnitLevel());
                    return levelRepository.save(newLevel);
                });
    }

    public List<DamEntity> findByClientAndStatus(Long clientId, Long statusId) {
        List<DamEntity> dams = damRepository.findWithDetailsByClientAndStatus(clientId, statusId);

        return dams;
    }

    @Transactional
    public void deleteById(Long id) {
        DamEntity dam = damRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada para exclusão!"));

        if (!dam.getChecklists().isEmpty() || !dam.getChecklistResponses().isEmpty() || !dam.getSections().isEmpty() || !dam.getDamPermissions().isEmpty() || !dam.getInstruments().isEmpty()) {
            throw new BusinessRuleException(
                    "Não é possível excluir a barragem devido as dependências existentes, recomenda-se inativar a barragem se necessário.");
        }

        damRepository.deleteById(id);
    }

    @Transactional
    public DamEntity updateBasicInfo(Long damId, UpdateDamRequest request) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getAttributionsPermission().getEditDam()) {
                throw new UnauthorizedException("Usuário não tem permissão para editar barragens!");
            }
        }

        DamEntity existingDam = damRepository.findById(damId)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + damId));

        ClientEntity client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com ID: " + request.getClientId()));

        if (!existingDam.getName().equals(request.getName()) || !existingDam.getClient().getId().equals(client.getId())) {
            if (damRepository.existsByNameAndClientIdAndIdNot(request.getName(), client.getId(), damId)) {
                throw new DuplicateResourceException("Já existe uma barragem com este nome para este cliente!");
            }
        }

        StatusEntity status = statusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new NotFoundException("Status não encontrado com ID: " + request.getStatusId()));

        existingDam.setName(request.getName());
        existingDam.setLatitude(request.getLatitude());
        existingDam.setLongitude(request.getLongitude());
        existingDam.setStreet(request.getStreet());
        existingDam.setNeighborhood(request.getNeighborhood());
        existingDam.setNumberAddress(request.getNumberAddress());
        existingDam.setCity(request.getCity());
        existingDam.setState(request.getState());
        existingDam.setZipCode(request.getZipCode());
        existingDam.setClient(client);
        existingDam.setStatus(status);
        existingDam.setLinkPSB(request.getLinkPSB());
        existingDam.setLinkLegislation(request.getLinkLegislation());

        if (request.getLogoBase64() != null && !request.getLogoBase64().isEmpty()) {
            if (existingDam.getLogoPath() != null) {
                fileStorageService.deleteFile(existingDam.getLogoPath());
            }

            String base64Image = request.getLogoBase64();
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            String logoUrl = fileStorageService.storeFileFromBytes(
                    imageBytes,
                    "logo_" + damId + ".jpg",
                    "image/jpeg",
                    "logos"
            );
            existingDam.setLogoPath(logoUrl);
        }

        if (request.getDamImageBase64() != null && !request.getDamImageBase64().isEmpty()) {
            if (existingDam.getDamImagePath() != null) {
                fileStorageService.deleteFile(existingDam.getDamImagePath());
            }

            String base64Image = request.getDamImageBase64();
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            String damImageUrl = fileStorageService.storeFileFromBytes(
                    imageBytes,
                    "dam_image_" + damId + ".jpg",
                    "image/jpeg",
                    "dam_images"
            );
            existingDam.setDamImagePath(damImageUrl);
        }

        DamEntity updatedDam = damRepository.save(existingDam);

        return findById(updatedDam.getId());
    }

    public List<DamEntity> findDamsByClientId(Long clientId) {
        List<DamEntity> damsWithFolders = damRepository.findWithPsbFoldersByClientId(clientId);
        List<DamEntity> damsWithReservoirs = damRepository.findWithReservoirsByClientId(clientId);

        for (DamEntity dam : damsWithFolders) {
            for (DamEntity damWithReservoirs : damsWithReservoirs) {
                if (dam.getId().equals(damWithReservoirs.getId())) {
                    dam.setReservoirs(damWithReservoirs.getReservoirs());
                    break;
                }
            }
        }

        return damsWithFolders;
    }

    public DamEntity findById(Long id) {
        DamEntity dam = damRepository.findWithPsbFoldersById(id)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));

        DamEntity damWithReservoirs = damRepository.findWithReservoirsById(id)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));

        dam.setReservoirs(damWithReservoirs.getReservoirs());

        return dam;
    }

    public List<DamEntity> findAll() {
        List<DamEntity> dams = damRepository.findAll();

        for (DamEntity dam : dams) {
            DamEntity damWithDetails = findById(dam.getId());
            dam.setPsbFolders(damWithDetails.getPsbFolders());
            dam.setReservoirs(damWithDetails.getReservoirs());
        }

        return dams;
    }

    public boolean existsByName(String name) {
        return damRepository.existsByName(name);
    }

    public boolean existsByNameAndIdNot(String name, Long id) {
        return damRepository.existsByNameAndIdNot(name, id);
    }
}
