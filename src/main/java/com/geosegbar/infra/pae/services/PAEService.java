package com.geosegbar.infra.pae.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.PAEZoneTypeEnum;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.PAEEntity;
import com.geosegbar.entities.PAEProtectionElementEntity;
import com.geosegbar.entities.PAEZoneContactEntity;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.pae.dtos.PAEContactDTO;
import com.geosegbar.infra.pae.dtos.PAEDTO;
import com.geosegbar.infra.pae.dtos.PAEProtectionElementDTO;
import com.geosegbar.infra.pae.dtos.PAEResponseDTO;
import com.geosegbar.infra.pae.persistence.jpa.PAERepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PAEService {

    private final PAERepository paeRepository;
    private final DamRepository damRepository;

    @Transactional(readOnly = true)
    public PAEResponseDTO findById(Long id) {
        PAEEntity pae = paeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PAE não encontrado com ID: " + id));
        return toResponseDTO(pae);
    }

    @Transactional(readOnly = true)
    public PAEResponseDTO findByDamId(Long damId) {
        PAEEntity pae = paeRepository.findByDamId(damId)
                .orElseThrow(() -> new NotFoundException("PAE não encontrado para a barragem com ID: " + damId));
        return toResponseDTO(pae);
    }

    @Transactional(readOnly = true)
    public List<PAEResponseDTO> findAll() {
        return paeRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PAEResponseDTO createOrUpdate(PAEDTO dto) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getAttributionsPermission().getEditPAE()) {
                throw new UnauthorizedException("Usuário não tem permissão para criar ou editar PAE!");
            }
        }

        DamEntity dam = damRepository.findById(dto.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + dto.getDamId()));

        PAEEntity pae;

        if (dto.getId() != null) {
            pae = paeRepository.findById(dto.getId())
                    .orElseThrow(() -> new NotFoundException("PAE não encontrado com ID: " + dto.getId()));

            if (!pae.getDam().getId().equals(dto.getDamId())) {
                throw new UnauthorizedException("Não é permitido mudar a barragem associada ao PAE");
            }
        } else {
            if (paeRepository.existsByDamId(dto.getDamId())) {
                throw new DuplicateResourceException("Já existe um PAE para a barragem com ID: " + dto.getDamId());
            }
            pae = new PAEEntity();
            pae.setDam(dam);
        }

        pae.setCoordinatorName(dto.getCoordinatorName());
        pae.setCoordinatorPhone(dto.getCoordinatorPhone());
        pae.setCoordinatorEmail(dto.getCoordinatorEmail());
        pae.setSubstituteCoordinatorName(dto.getSubstituteCoordinatorName());
        pae.setSubstituteCoordinatorPhone(dto.getSubstituteCoordinatorPhone());
        pae.setSubstituteCoordinatorEmail(dto.getSubstituteCoordinatorEmail());
        pae.setResidences(dto.getResidences());
        pae.setPeople(dto.getPeople());
        pae.setSensiblePoints(dto.getSensiblePoints());
        pae.setLastCadastralSurvey(dto.getLastCadastralSurvey());
        pae.setSimulationParticipants(dto.getSimulationParticipants());
        pae.setLastSimulationDate(dto.getLastSimulationDate());

        if (dto.getProtectionElements() != null) {
            pae.getProtectionElements().clear();
            for (PAEProtectionElementDTO elementDto : dto.getProtectionElements()) {
                PAEProtectionElementEntity element = new PAEProtectionElementEntity();
                element.setPae(pae);
                element.setName(elementDto.getName());
                element.setValue(elementDto.getValue());
                pae.getProtectionElements().add(element);
            }
        }

        if (dto.getContacts() != null) {
            pae.getContacts().clear();
            for (PAEContactDTO contactDto : dto.getContacts()) {
                PAEZoneContactEntity contact = new PAEZoneContactEntity();
                contact.setPae(pae);
                contact.setZone(contactDto.getZone());
                contact.setName(contactDto.getName());
                contact.setRole(contactDto.getRole());
                contact.setCity(contactDto.getCity());
                contact.setState(contactDto.getState());
                contact.setPhone(contactDto.getPhone());
                contact.setEmail(contactDto.getEmail());
                pae.getContacts().add(contact);
            }
        }

        PAEEntity saved = paeRepository.save(pae);

        log.info("PAE {} para a barragem {}",
                dto.getId() == null ? "criado" : "atualizado",
                dam.getName());

        return toResponseDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getAttributionsPermission().getEditPAE()) {
                throw new UnauthorizedException("Usuário não tem permissão para excluir PAE!");
            }
        }

        PAEEntity pae = paeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("PAE não encontrado com ID: " + id));

        log.info("PAE excluído para a barragem {}", pae.getDam().getName());

        paeRepository.delete(pae);
    }

    private PAEResponseDTO toResponseDTO(PAEEntity pae) {
        PAEResponseDTO dto = new PAEResponseDTO();
        dto.setId(pae.getId());
        dto.setDamId(pae.getDam().getId());
        dto.setDamName(pae.getDam().getName());
        dto.setCoordinatorName(pae.getCoordinatorName());
        dto.setCoordinatorPhone(pae.getCoordinatorPhone());
        dto.setCoordinatorEmail(pae.getCoordinatorEmail());
        dto.setSubstituteCoordinatorName(pae.getSubstituteCoordinatorName());
        dto.setSubstituteCoordinatorPhone(pae.getSubstituteCoordinatorPhone());
        dto.setSubstituteCoordinatorEmail(pae.getSubstituteCoordinatorEmail());
        dto.setResidences(pae.getResidences());
        dto.setPeople(pae.getPeople());
        dto.setSensiblePoints(pae.getSensiblePoints());
        dto.setLastCadastralSurvey(pae.getLastCadastralSurvey());
        dto.setSimulationParticipants(pae.getSimulationParticipants());
        dto.setLastSimulationDate(pae.getLastSimulationDate());

        dto.setProtectionElements(pae.getProtectionElements().stream()
                .map(e -> new PAEProtectionElementDTO(e.getId(), e.getName(), e.getValue()))
                .collect(Collectors.toList()));

        dto.setZasContacts(pae.getContacts().stream()
                .filter(c -> PAEZoneTypeEnum.ZAS.equals(c.getZone()))
                .map(this::toContactDTO)
                .collect(Collectors.toList()));

        dto.setZssContacts(pae.getContacts().stream()
                .filter(c -> PAEZoneTypeEnum.ZSS.equals(c.getZone()))
                .map(this::toContactDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    private PAEContactDTO toContactDTO(PAEZoneContactEntity contact) {
        return new PAEContactDTO(
                contact.getId(),
                contact.getZone(),
                contact.getName(),
                contact.getRole(),
                contact.getCity(),
                contact.getState(),
                contact.getPhone(),
                contact.getEmail()
        );
    }
}
