package com.geosegbar.infra.permissions.dam_permissions.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.permissions.dam_permissions.dtos.DamPermissionDTO;
import com.geosegbar.infra.permissions.dam_permissions.dtos.UserDamPermissionsRequestDTO;
import com.geosegbar.infra.permissions.dam_permissions.persistence.DamPermissionRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DamPermissionService {

    private final DamPermissionRepository damPermissionRepository;
    private final UserRepository userRepository;
    private final DamRepository damRepository;
    private final ClientRepository clientRepository;

    /**
     * Retorna todas as permissões de barragem para os clientes do usuário. Se
     * uma permissão não existir para uma barragem válida, ela é criada
     * automaticamente (hasAccess=false).
     *
     * Lógica Original Preservada: Auto-criação de permissões faltantes.
     */
    @Transactional
    public List<DamPermissionEntity> findAllDamPermissionsForUserClients(Long userId) {

        UserEntity user = userRepository.findByIdWithClients(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

        Set<ClientEntity> userClients = user.getClients();
        if (userClients == null || userClients.isEmpty()) {
            return new ArrayList<>();
        }

        List<DamPermissionEntity> existingPermissions = damPermissionRepository.findByUser(user);

        Map<Long, DamPermissionEntity> permissionsMap = existingPermissions.stream()
                .collect(Collectors.toMap(p -> p.getDam().getId(), Function.identity()));

        List<DamPermissionEntity> allPermissions = new ArrayList<>();
        List<DamPermissionEntity> newPermissionsToSave = new ArrayList<>();

        for (ClientEntity client : userClients) {

            List<DamEntity> clientDams = damRepository.findByClientId(client.getId());

            for (DamEntity dam : clientDams) {
                if (permissionsMap.containsKey(dam.getId())) {
                    allPermissions.add(permissionsMap.get(dam.getId()));
                } else {
                    DamPermissionEntity newPermission = new DamPermissionEntity();
                    newPermission.setUser(user);
                    newPermission.setDam(dam);
                    newPermission.setClient(client);
                    newPermission.setHasAccess(false);
                    newPermission.setCreatedAt(LocalDateTime.now());

                    newPermissionsToSave.add(newPermission);
                    allPermissions.add(newPermission);
                }
            }
        }

        if (!newPermissionsToSave.isEmpty()) {
            damPermissionRepository.saveAll(newPermissionsToSave);
        }

        return allPermissions;
    }

    @Transactional(readOnly = true)
    public boolean checkUserHasAccessToDam(Long userId, Long damId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

        DamEntity dam = damRepository.findById(damId)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + damId));

        ClientEntity client = dam.getClient();
        if (client == null) {
            throw new NotFoundException("Barragem não está associada a nenhum cliente");
        }

        if (!user.getClients().contains(client)) {
            return false;
        }

        var permission = damPermissionRepository.findByUserAndDamAndClient(user, dam, client);
        if (permission.isPresent()) {
            return permission.get().getHasAccess();
        }

        return user.getRole() != null && "ADMIN".equals(user.getRole().getName().name());
    }

    /**
     * Configura permissões iniciais. Funcionalmente idêntico ao
     * updatePermissionsForUser na lógica original (upsert), mas mantido
     * separado se houver distinção semântica futura.
     */
    @Transactional
    public List<DamPermissionEntity> setupPermissionsForUser(Long userId, UserDamPermissionsRequestDTO requestDTO) {
        return processPermissionsUpdate(userId, requestDTO, false);

    }

    /**
     * Atualiza permissões. Lógica Original Preservada: 1. Atualiza/Cria as
     * enviadas no DTO. 2. Revoga (hasAccess=false) as que o usuário já tinha
     * mas NÃO vieram no DTO.
     */
    @Transactional
    public List<DamPermissionEntity> updatePermissionsForUser(Long userId, UserDamPermissionsRequestDTO requestDTO) {
        return processPermissionsUpdate(userId, requestDTO, true);

    }

    /**
     * Método centralizado para processar atualizações, evitando código
     * duplicado.
     */
    private List<DamPermissionEntity> processPermissionsUpdate(Long userId, UserDamPermissionsRequestDTO requestDTO, boolean revokeMissing) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + userId));

        List<DamPermissionEntity> allUserPermissions = findAllDamPermissionsForUserClients(userId);

        Map<Long, DamPermissionEntity> permissionMap = allUserPermissions.stream()
                .collect(Collectors.toMap(p -> p.getDam().getId(), Function.identity()));

        List<DamPermissionEntity> updatedPermissions = new ArrayList<>();
        Set<Long> processedDamIds = new HashSet<>();

        for (DamPermissionDTO dto : requestDTO.getPermissions()) {

            DamEntity dam = damRepository.findById(dto.getDamId())
                    .orElseThrow(() -> new NotFoundException("Barragem não encontrada: " + dto.getDamId()));

            ClientEntity client = clientRepository.findById(dto.getClientId())
                    .orElseThrow(() -> new NotFoundException("Cliente não encontrado: " + dto.getClientId()));

            if (!user.getClients().contains(client)) {
                throw new InvalidInputException("O cliente " + client.getName() + " não está associado ao usuário!");
            }
            if (dam.getClient() == null || !dam.getClient().getId().equals(client.getId())) {
                throw new InvalidInputException("A barragem " + dam.getName() + " não está associada ao cliente!");
            }

            if (processedDamIds.contains(dto.getDamId())) {
                continue;
            }
            processedDamIds.add(dto.getDamId());

            DamPermissionEntity permission = permissionMap.get(dto.getDamId());

            if (permission == null) {
                permission = new DamPermissionEntity();
                permission.setUser(user);
                permission.setDam(dam);
                permission.setClient(client);
                permission.setCreatedAt(LocalDateTime.now());
            }

            permission.setHasAccess(dto.getHasAccess());
            permission.setUpdatedAt(LocalDateTime.now());

            updatedPermissions.add(permission);
        }

        if (revokeMissing) {
            for (DamPermissionEntity existing : allUserPermissions) {

                if (!processedDamIds.contains(existing.getDam().getId())) {
                    existing.setHasAccess(false);
                    existing.setUpdatedAt(LocalDateTime.now());
                    updatedPermissions.add(existing);
                }
            }
        }

        return damPermissionRepository.saveAll(updatedPermissions);
    }

    @Transactional
    public void delete(Long id) {
        if (!damPermissionRepository.existsById(id)) {
            throw new NotFoundException("Permissão não encontrada com ID: " + id);
        }
        damPermissionRepository.deleteById(id);
    }

    @Transactional
    public void createDefaultPermissionsForDam(DamEntity dam) {
        ClientEntity client = dam.getClient();
        if (client == null) {
            return;
        }

        List<UserEntity> users = userRepository.findByClientId(client.getId());
        List<DamPermissionEntity> toSave = new ArrayList<>();

        for (UserEntity user : users) {
            if (!damPermissionRepository.existsByUserAndDamAndClient(user, dam, client)) {
                DamPermissionEntity p = new DamPermissionEntity();
                p.setUser(user);
                p.setDam(dam);
                p.setClient(client);
                p.setHasAccess(false);
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                toSave.add(p);
            }
        }
        if (!toSave.isEmpty()) {
            damPermissionRepository.saveAll(toSave);
        }
    }

    @Transactional
    public void removeAllPermissionsForDam(Long damId) {
        damPermissionRepository.deleteByDamId(damId);
    }

    @Transactional
    public void syncPermissionsOnClientChange(DamEntity dam, Long oldClientId) {
        if (oldClientId != null) {
            damPermissionRepository.deleteByDamIdAndClientId(dam.getId(), oldClientId);
        }
        createDefaultPermissionsForDam(dam);
    }

    @Transactional
    public DamPermissionEntity updatePermission(DamPermissionDTO dto) {
        UserDamPermissionsRequestDTO req = new UserDamPermissionsRequestDTO();
        req.setPermissions(List.of(dto));
        List<DamPermissionEntity> result = updatePermissionsForUser(dto.getUserId(), req);
        return result.isEmpty() ? null : result.get(0);
    }
}
