package com.geosegbar.infra.dam.services;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.StatusEnum;
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
import com.geosegbar.infra.classification_dam.persistence.ClassificationDamRepository;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.dam.dtos.CreateDamCompleteRequest;
import com.geosegbar.infra.dam.dtos.DamMapDataDTO;
import com.geosegbar.infra.dam.dtos.DamQuickAccessDTO;
import com.geosegbar.infra.dam.dtos.DamStatusUpdateDTO;
import com.geosegbar.infra.dam.dtos.LevelRequestDTO;
import com.geosegbar.infra.dam.dtos.MapAnomalyDTO;
import com.geosegbar.infra.dam.dtos.MapInstrumentDTO;
import com.geosegbar.infra.dam.dtos.MapSectionDTO;
import com.geosegbar.infra.dam.dtos.ReservoirRequestDTO;
import com.geosegbar.infra.dam.dtos.UpdateDamCompleteRequest;
import com.geosegbar.infra.dam.dtos.UpdateDamRequest;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.dam.projections.DamQuickAccessProjection;
import com.geosegbar.infra.anomaly.persistence.jpa.AnomalyRepository;
import com.geosegbar.infra.documentation_dam.persistence.DocumentationDamRepository;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.level.persistence.LevelRepository;
import com.geosegbar.infra.potential_damage.persistence.PotentialDamageRepository;
import com.geosegbar.infra.psb.persistence.PSBFolderRepository;
import com.geosegbar.infra.psb.services.PSBFolderService;
import com.geosegbar.infra.regulatory_dam.persistence.RegulatoryDamRepository;
import com.geosegbar.infra.reservoir.persistence.ReservoirRepository;
import com.geosegbar.infra.risk_category.persistence.RiskCategoryRepository;
import com.geosegbar.infra.section.persistence.jpa.SectionRepository;
import com.geosegbar.infra.security_level.persistence.SecurityLevelRepository;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;

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
    private final PSBFolderRepository psbFolderRepository;
    private final PSBFolderService psbFolderService;
    private final com.geosegbar.infra.permissions.dam_permissions.services.DamPermissionService damPermissionService;
    private final InstrumentRepository instrumentRepository;
    private final SectionRepository sectionRepository;
    private final AnomalyRepository anomalyRepository;

    @Transactional(readOnly = true)
    public DamMapDataDTO getMapData(Long damId) {
        if (!damRepository.existsById(damId)) {
            throw new NotFoundException("Barragem não encontrada com ID: " + damId);
        }

        List<MapInstrumentDTO> instruments = instrumentRepository.findMapDataByDamId(damId);
        List<MapSectionDTO> sections = sectionRepository.findMapDataByDamId(damId);

        List<Object[]> anomalyRows = anomalyRepository.findMapDataByDamId(damId);
        List<MapAnomalyDTO> anomalies = anomalyRows.stream().map(row -> new MapAnomalyDTO(
                row[0] != null ? ((Number) row[0]).longValue() : null,
                row[1] != null ? ((Number) row[1]).doubleValue() : null,
                row[2] != null ? ((Number) row[2]).doubleValue() : null,
                row[3] != null ? ((Number) row[3]).longValue() : null,
                (String) row[4],
                row[5] != null ? ((Number) row[5]).longValue() : null,
                (String) row[6],
                (String) row[7],
                (String) row[8]
        )).collect(Collectors.toList());

        return new DamMapDataDTO(instruments, sections, anomalies);
    }

    @Transactional(readOnly = true)
    public DamEntity findById(Long id) {
        DamEntity dam = damRepository.findByIdComplete(id)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada!"));

        initializeLazyCollections(dam);
        return dam;
    }

    @Transactional(readOnly = true)
    public DamEntity findByIdWithSections(Long id) {
        return findById(id);
    }

    @Transactional(readOnly = true)
    public List<DamEntity> findAll() {
        List<DamEntity> dams = damRepository.findAllComplete();

        dams.forEach(this::initializeLazyCollections);
        return dams;
    }

    @Transactional(readOnly = true)
    public List<DamEntity> findAllWithSections() {
        return findAll();
    }

    @Transactional(readOnly = true)
    public List<DamEntity> findDamsByClientId(Long clientId) {
        return findByClientAndStatus(clientId, null);
    }

    @Transactional(readOnly = true)
    public List<DamEntity> findDamsByClientIdWithSections(Long clientId) {
        return findDamsByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public List<DamEntity> findByClientAndStatus(Long clientId, Long statusId) {

        List<DamEntity> dams = damRepository.findByClientAndStatusComplete(clientId, statusId);

        dams.forEach(this::initializeLazyCollections);

        return dams;
    }

    @Transactional(readOnly = true)
    public List<DamEntity> findByClientAndStatusWithSections(Long clientId, Long statusId) {
        return findByClientAndStatus(clientId, statusId);
    }

    @Transactional(readOnly = true)
    public List<DamQuickAccessDTO> findQuickAccessByCurrentUser() {
        UserEntity currentUser = AuthenticatedUserUtil.getCurrentUser();

        List<DamQuickAccessProjection> rows = AuthenticatedUserUtil.isAdmin()
                ? damRepository.findAllQuickAccess()
                : damRepository.findQuickAccessByUserId(currentUser.getId());

        return rows.stream()
                .map(this::toQuickAccessDTO)
                .toList();
    }

    private void initializeLazyCollections(DamEntity dam) {
        if (dam == null) {
            return;
        }

        Hibernate.initialize(dam.getSections());

        Hibernate.initialize(dam.getReservoirs());
        if (dam.getReservoirs() != null) {
            dam.getReservoirs().forEach(r -> Hibernate.initialize(r.getLevel()));
        }

        Hibernate.initialize(dam.getPsbFolders());

    }

    private DamQuickAccessDTO toQuickAccessDTO(DamQuickAccessProjection row) {
        return new DamQuickAccessDTO(
                row.getDamId(),
                row.getDamName(),
                StatusEnum.valueOf(row.getStatus()),
                row.getClientId(),
                row.getClientName()
        );
    }

    public boolean existsByName(String name) {
        return damRepository.existsByName(name);
    }

    public boolean existsByNameAndIdNot(String name, Long id) {
        return damRepository.existsByNameAndIdNot(name, id);
    }

    @Transactional
    public int synchronizeClientDamsStatus(Long clientId, StatusEntity status) {
        return damRepository.updateStatusByClientId(clientId, status);
    }

    @Transactional
    public DamEntity updateStatus(Long damId, DamStatusUpdateDTO statusUpdateDTO) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getAttributionsPermission().getEditDam()) {
                throw new UnauthorizedException("Usuário não tem permissão para modificar status de barragens!");
            }
        }

        DamEntity dam = damRepository.findById(damId)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada"));

        StatusEntity status = statusRepository.findById(statusUpdateDTO.getStatusId())
                .orElseThrow(() -> new NotFoundException("Status não encontrado com ID: " + statusUpdateDTO.getStatusId()));

        dam.setStatus(status);
        damRepository.save(dam);

        return findById(damId);
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
        dam.setComplement(request.getComplement());
        dam.setClient(client);
        dam.setStatus(status);
        dam.setPsbLinkFolder(resolvePSBFolderFromRequest(request.getPsbLinkFolderId()));
        dam.setLegislationLinkFolder(resolvePSBFolderFromRequest(request.getLegislationLinkFolderId()));

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

        DamEntity savedDam = findById(dam.getId());

        damPermissionService.createDefaultPermissionsForDam(savedDam);

        return savedDam;
    }

    @Transactional
    public void deleteById(Long id) {
        DamEntity dam = damRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada para exclusão!"));

        if (dam.getChecklist() != null || !dam.getChecklistResponses().isEmpty()
                || !dam.getSections().isEmpty() || !dam.getDamPermissions().isEmpty()
                || !dam.getInstruments().isEmpty()) {
            throw new BusinessRuleException(
                    "Não é possível excluir a barragem devido as dependências existentes, recomenda-se inativar a barragem se necessário.");
        }

        damPermissionService.removeAllPermissionsForDam(id);
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

        Long oldClientId = existingDam.getClient().getId();

        if (!existingDam.getName().equals(request.getName()) || !existingDam.getClient().getId().equals(client.getId())) {
            if (damRepository.existsByNameAndClientIdAndIdNot(request.getName(), client.getId(), damId)) {
                throw new DuplicateResourceException("Já existe uma barragem com este nome para este cliente!");
            }
        }

        StatusEntity status = statusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new NotFoundException("Status não encontrado com ID: " + request.getStatusId()));

        boolean clientChanged = !oldClientId.equals(client.getId());

        existingDam.setName(request.getName());
        existingDam.setLatitude(request.getLatitude());
        existingDam.setLongitude(request.getLongitude());
        existingDam.setStreet(request.getStreet());
        existingDam.setNeighborhood(request.getNeighborhood());
        existingDam.setNumberAddress(request.getNumberAddress());
        existingDam.setCity(request.getCity());
        existingDam.setState(request.getState());
        existingDam.setZipCode(request.getZipCode());
        existingDam.setComplement(request.getComplement());
        existingDam.setClient(client);
        existingDam.setStatus(status);
        existingDam.setPsbLinkFolder(resolvePSBFolderFromRequest(request.getPsbLinkFolderId()));
        existingDam.setLegislationLinkFolder(resolvePSBFolderFromRequest(request.getLegislationLinkFolderId()));

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

        if (clientChanged) {
            damPermissionService.syncPermissionsOnClientChange(updatedDam, oldClientId);
        }

        return findById(updatedDam.getId());
    }

    @Transactional
    public DamEntity updateCompleteWithRelationships(Long damId, UpdateDamCompleteRequest request) {

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

        StatusEntity status = statusRepository.findById(request.getStatusId())
                .orElseThrow(() -> new NotFoundException("Status não encontrado com ID: " + request.getStatusId()));

        Long oldClientId = existingDam.getClient().getId();

        if (!existingDam.getName().equals(request.getName()) || !existingDam.getClient().getId().equals(client.getId())) {
            if (damRepository.existsByNameAndClientIdAndIdNot(request.getName(), client.getId(), damId)) {
                throw new DuplicateResourceException("Já existe uma barragem com este nome para este cliente!");
            }
        }

        boolean clientChanged = !oldClientId.equals(client.getId());

        existingDam.setName(request.getName());
        existingDam.setLatitude(request.getLatitude());
        existingDam.setLongitude(request.getLongitude());
        existingDam.setStreet(request.getStreet());
        existingDam.setNeighborhood(request.getNeighborhood());
        existingDam.setNumberAddress(request.getNumberAddress());
        existingDam.setCity(request.getCity());
        existingDam.setState(request.getState());
        existingDam.setZipCode(request.getZipCode());
        existingDam.setComplement(request.getComplement());
        existingDam.setClient(client);
        existingDam.setStatus(status);
        existingDam.setPsbLinkFolder(resolvePSBFolderFromRequest(request.getPsbLinkFolderId()));
        existingDam.setLegislationLinkFolder(resolvePSBFolderFromRequest(request.getLegislationLinkFolderId()));

        if (request.getLogoBase64() == null) {
            if (existingDam.getLogoPath() != null) {
                fileStorageService.deleteFile(existingDam.getLogoPath());
                existingDam.setLogoPath(null);
            }
        } else if (!request.getLogoBase64().isEmpty()) {
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

        if (request.getDamImageBase64() == null) {
            if (existingDam.getDamImagePath() != null) {
                fileStorageService.deleteFile(existingDam.getDamImagePath());
                existingDam.setDamImagePath(null);
            }
        } else if (!request.getDamImageBase64().isEmpty()) {
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

        DamEntity savedDam = damRepository.save(existingDam);

        final DamEntity finalDam = savedDam;

        DocumentationDamEntity documentationDam = documentationDamRepository.findByDamId(damId)
                .orElseGet(() -> {
                    DocumentationDamEntity newDoc = new DocumentationDamEntity();
                    newDoc.setDam(finalDam);
                    return newDoc;
                });

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

        RegulatoryDamEntity regulatoryDam = regulatoryDamRepository.findByDamId(damId)
                .orElseGet(() -> {
                    RegulatoryDamEntity newReg = new RegulatoryDamEntity();
                    newReg.setDam(finalDam);
                    return newReg;
                });

        regulatoryDam.setFramePNSB(request.getFramePNSB());
        regulatoryDam.setRepresentativeName(request.getRepresentativeName());
        regulatoryDam.setRepresentativeEmail(request.getRepresentativeEmail());
        regulatoryDam.setRepresentativePhone(request.getRepresentativePhone());
        regulatoryDam.setTechnicalManagerName(request.getTechnicalManagerName());
        regulatoryDam.setTechnicalManagerEmail(request.getTechnicalManagerEmail());
        regulatoryDam.setTechnicalManagerPhone(request.getTechnicalManagerPhone());
        regulatoryDam.setSupervisoryBodyName(request.getSupervisoryBodyName());

        if (request.getSecurityLevelId() != null) {
            SecurityLevelEntity securityLevel = securityLevelRepository.findById(request.getSecurityLevelId())
                    .orElseThrow(() -> new NotFoundException("Nível de segurança não encontrado"));
            regulatoryDam.setSecurityLevel(securityLevel);
        } else {
            regulatoryDam.setSecurityLevel(null);
        }

        if (request.getRiskCategoryId() != null) {
            RiskCategoryEntity riskCategory = riskCategoryRepository.findById(request.getRiskCategoryId())
                    .orElseThrow(() -> new NotFoundException("Categoria de risco não encontrada"));
            regulatoryDam.setRiskCategory(riskCategory);
        } else {
            regulatoryDam.setRiskCategory(null);
        }

        if (request.getPotentialDamageId() != null) {
            PotentialDamageEntity potentialDamage = potentialDamageRepository.findById(request.getPotentialDamageId())
                    .orElseThrow(() -> new NotFoundException("Dano potencial não encontrado"));
            regulatoryDam.setPotentialDamage(potentialDamage);
        } else {
            regulatoryDam.setPotentialDamage(null);
        }

        if (request.getClassificationDamId() != null) {
            ClassificationDamEntity classificationDam = classificationDamRepository.findById(request.getClassificationDamId())
                    .orElseThrow(() -> new NotFoundException("Classificação da barragem não encontrada"));
            regulatoryDam.setClassificationDam(classificationDam);
        } else {
            regulatoryDam.setClassificationDam(null);
        }

        regulatoryDamRepository.save(regulatoryDam);

        if (request.getReservoirs() != null) {
            List<ReservoirEntity> currentReservoirs = reservoirRepository.findByDamIdOrderByCreatedAtDesc(damId);
            Set<Long> receivedReservoirIds = new HashSet<>();

            for (ReservoirRequestDTO reservoirDTO : request.getReservoirs()) {
                if (reservoirDTO.getId() != null) {
                    ReservoirEntity existingReservoir = reservoirRepository.findById(reservoirDTO.getId())
                            .orElseThrow(() -> new NotFoundException("Reservatório não encontrado com ID: " + reservoirDTO.getId()));

                    if (!existingReservoir.getDam().getId().equals(damId)) {
                        throw new BusinessRuleException("Reservatório com ID " + reservoirDTO.getId() + " não pertence a esta barragem!");
                    }

                    LevelEntity level = processLevelForUpdate(reservoirDTO.getLevel(), existingReservoir.getLevel());
                    existingReservoir.setLevel(level);

                    reservoirRepository.save(existingReservoir);
                    receivedReservoirIds.add(reservoirDTO.getId());
                } else {
                    LevelEntity level = processLevel(reservoirDTO.getLevel());
                    ReservoirEntity newReservoir = new ReservoirEntity();
                    newReservoir.setDam(finalDam);
                    newReservoir.setLevel(level);

                    ReservoirEntity savedReservoir = reservoirRepository.save(newReservoir);
                    receivedReservoirIds.add(savedReservoir.getId());
                }
            }

            List<ReservoirEntity> reservoirsToDelete = currentReservoirs.stream()
                    .filter(reservoir -> !receivedReservoirIds.contains(reservoir.getId()))
                    .collect(java.util.stream.Collectors.toList());

            if (!reservoirsToDelete.isEmpty()) {
                reservoirRepository.deleteAll(reservoirsToDelete);
            }
        }

        if (request.getPsbFolders() != null && !request.getPsbFolders().isEmpty()) {
            UserEntity currentUser = AuthenticatedUserUtil.getCurrentUser();
            psbFolderService.syncRootFolders(finalDam, request.getPsbFolders(), currentUser.getId());
        }

        if (clientChanged) {
            damPermissionService.syncPermissionsOnClientChange(finalDam, oldClientId);
        }

        return findById(damId);
    }

    private PSBFolderEntity resolvePSBFolderFromRequest(Long folderId) {
        if (folderId == null) {
            return null;
        }
        return psbFolderRepository.findById(folderId)
                .orElseThrow(() -> new NotFoundException("PSB Folder não encontrado com ID: " + folderId));
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

    private LevelEntity processLevelForUpdate(LevelRequestDTO levelDTO, LevelEntity currentLevel) {
        if (levelDTO.getId() != null) {
            return levelRepository.findById(levelDTO.getId())
                    .orElseThrow(() -> new NotFoundException("Nível não encontrado com ID: " + levelDTO.getId()));
        }

        if (!currentLevel.getName().equals(levelDTO.getName())) {
            return levelRepository.findByName(levelDTO.getName())
                    .orElseGet(() -> {
                        LevelEntity newLevel = new LevelEntity();
                        newLevel.setName(levelDTO.getName());
                        newLevel.setValue(levelDTO.getValue());
                        newLevel.setUnitLevel(levelDTO.getUnitLevel());
                        return levelRepository.save(newLevel);
                    });
        }

        currentLevel.setValue(levelDTO.getValue());
        currentLevel.setUnitLevel(levelDTO.getUnitLevel());
        return levelRepository.save(currentLevel);
    }
}
