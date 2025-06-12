package com.geosegbar.infra.section.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.SectionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.infra.file_storage.FileStorageService;
import com.geosegbar.infra.section.dtos.CreateSectionDTO;
import com.geosegbar.infra.section.persistence.jpa.SectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionService {

    private final SectionRepository sectionRepository;
    private final FileStorageService fileStorageService;

    public List<SectionEntity> findAll() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewSections()) {
                throw new UnauthorizedException("Usuário não autorizado a visualizar seções!");
            }
        }
        return sectionRepository.findAllByOrderByNameAsc();
    }

    public SectionEntity findById(Long id) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewSections()) {
                throw new UnauthorizedException("Usuário não autorizado a visualizar seções!");
            }
        }
        return sectionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Seção não encontrada com ID: " + id));
    }

    public Optional<SectionEntity> findByName(String name) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewSections()) {
                throw new UnauthorizedException("Usuário não autorizado a visualizar seções!");
            }
        }
        return sectionRepository.findByName(name);
    }

    @Transactional
    public SectionEntity create(SectionEntity section) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditSections()) {
                throw new UnauthorizedException("Usuário não autorizado a criar seções!");
            }
        }
        if (sectionRepository.findByName(section.getName()).isPresent()) {
            throw new DuplicateResourceException("Seção com nome '" + section.getName() + "' já existe");
        }

        SectionEntity savedSection = sectionRepository.save(section);
        log.info("Nova seção criada: {}", savedSection.getName());
        return savedSection;
    }

    @Transactional
    public SectionEntity createWithFile(CreateSectionDTO dto, MultipartFile file) {
        if (sectionRepository.findByName(dto.getName()).isPresent()) {
            throw new DuplicateResourceException("Seção com nome '" + dto.getName() + "' já existe");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!extension.equals("dwg") && !extension.equals("dxf")) {
                throw new InvalidInputException("Tipo de arquivo não suportado. Apenas arquivos .dwg e .dxf são permitidos.");
            }
        } else {
            throw new InvalidInputException("Nome de arquivo inválido");
        }

        String filePath = fileStorageService.storeFile(file, "sections");

        SectionEntity section = new SectionEntity();
        section.setName(dto.getName());
        section.setFilePath(filePath);
        section.setFirstVertexLatitude(dto.getFirstVertexLatitude());
        section.setSecondVertexLatitude(dto.getSecondVertexLatitude());
        section.setFirstVertexLongitude(dto.getFirstVertexLongitude());
        section.setSecondVertexLongitude(dto.getSecondVertexLongitude());

        SectionEntity savedSection = sectionRepository.save(section);
        log.info("Nova seção criada com arquivo: {}", savedSection.getName());
        return savedSection;
    }

    @Transactional
    public SectionEntity updateWithFile(Long id, CreateSectionDTO dto, MultipartFile file) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditSections()) {
                throw new UnauthorizedException("Usuário não autorizado a editar seções!");
            }
        }
        SectionEntity existingSection = findById(id);

        if (sectionRepository.findByName(dto.getName()).isPresent()
                && !existingSection.getName().equals(dto.getName())) {
            throw new DuplicateResourceException("Seção com nome '" + dto.getName() + "' já existe");
        }

        if (existingSection.getFilePath() != null && !existingSection.getFilePath().isEmpty()) {
            fileStorageService.deleteFile(existingSection.getFilePath());
        }

        if (file != null && !file.isEmpty()) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null) {
                String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                if (!extension.equals("dwg") && !extension.equals("dxf")) {
                    throw new InvalidInputException("Tipo de arquivo não suportado. Apenas arquivos .dwg e .dxf são permitidos.");
                }
            } else {
                throw new InvalidInputException("Nome de arquivo inválido");
            }

            String filePath = fileStorageService.storeFile(file, "sections");
            existingSection.setFilePath(filePath);
        }

        existingSection.setName(dto.getName());
        existingSection.setFirstVertexLatitude(dto.getFirstVertexLatitude());
        existingSection.setSecondVertexLatitude(dto.getSecondVertexLatitude());
        existingSection.setFirstVertexLongitude(dto.getFirstVertexLongitude());
        existingSection.setSecondVertexLongitude(dto.getSecondVertexLongitude());

        SectionEntity updatedSection = sectionRepository.save(existingSection);
        log.info("Seção atualizada com arquivo: {}", updatedSection.getName());
        return updatedSection;
    }

    @Transactional
    public SectionEntity update(Long id, SectionEntity section) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditSections()) {
                throw new UnauthorizedException("Usuário não autorizado a editar seções!");
            }
        }
        SectionEntity existingSection = findById(id);

        if (sectionRepository.findByName(section.getName()).isPresent()
                && !existingSection.getName().equals(section.getName())) {
            throw new DuplicateResourceException("Seção com nome '" + section.getName() + "' já existe");
        }

        existingSection.setName(section.getName());
        existingSection.setFilePath(section.getFilePath());
        existingSection.setFirstVertexLatitude(section.getFirstVertexLatitude());
        existingSection.setSecondVertexLatitude(section.getSecondVertexLatitude());
        existingSection.setFirstVertexLongitude(section.getFirstVertexLongitude());
        existingSection.setSecondVertexLongitude(section.getSecondVertexLongitude());

        SectionEntity updatedSection = sectionRepository.save(existingSection);
        log.info("Seção atualizada: {}", updatedSection.getName());
        return updatedSection;
    }

    @Transactional
    public void delete(Long id) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditSections()) {
                throw new UnauthorizedException("Usuário não autorizado a excluir seções!");
            }
        }
        SectionEntity section = findById(id);

        if (!section.getInstruments().isEmpty()) {
            throw new IllegalStateException("Não é possível excluir uma seção que possui instrumentos associados");
        }

        if (section.getFilePath() != null && !section.getFilePath().isEmpty()) {
            fileStorageService.deleteFile(section.getFilePath());
        }

        sectionRepository.delete(section);
        log.info("Seção excluída: {}", section.getName());
    }
}
