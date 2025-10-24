package com.geosegbar.infra.user.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Hibernate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.geosegbar.common.email.EmailService;
import com.geosegbar.common.enums.RoleEnum;
import com.geosegbar.common.enums.StatusEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.common.utils.GenerateRandomCode;
import com.geosegbar.common.utils.GenerateRandomPassword;
import com.geosegbar.configs.security.TokenService;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.RoleEntity;
import com.geosegbar.entities.SexEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.entities.VerificationCodeEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.permissions.atributions_permission.dtos.AttributionsPermissionDTO;
import com.geosegbar.infra.permissions.atributions_permission.services.AttributionsPermissionService;
import com.geosegbar.infra.permissions.dam_permissions.persistence.DamPermissionRepository;
import com.geosegbar.infra.permissions.documentation_permission.dtos.DocumentationPermissionDTO;
import com.geosegbar.infra.permissions.documentation_permission.services.DocumentationPermissionService;
import com.geosegbar.infra.permissions.instrumentation_permission.dtos.InstrumentationPermissionDTO;
import com.geosegbar.infra.permissions.instrumentation_permission.services.InstrumentationPermissionService;
import com.geosegbar.infra.permissions.routine_inspection_permission.dtos.RoutineInspectionPermissionDTO;
import com.geosegbar.infra.permissions.routine_inspection_permission.services.RoutineInspectionPermissionService;
import com.geosegbar.infra.roles.persistence.RoleRepository;
import com.geosegbar.infra.sex.persistence.jpa.SexRepository;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;
import com.geosegbar.infra.user.dto.LoginRequestDTO;
import com.geosegbar.infra.user.dto.LoginResponseDTO;
import com.geosegbar.infra.user.dto.UserClientAssociationDTO;
import com.geosegbar.infra.user.dto.UserCreateDTO;
import com.geosegbar.infra.user.dto.UserPasswordUpdateDTO;
import com.geosegbar.infra.user.dto.UserStatusUpdateDTO;
import com.geosegbar.infra.user.dto.UserUpdateDTO;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;
import com.geosegbar.infra.verification_code.dto.ForgotPasswordRequestDTO;
import com.geosegbar.infra.verification_code.dto.ResetPasswordRequestDTO;
import com.geosegbar.infra.verification_code.dto.VerifyCodeRequestDTO;
import com.geosegbar.infra.verification_code.persistence.jpa.VerificationCodeRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final SexRepository sexRepository;
    private final StatusRepository statusRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;
    private final DamRepository damRepository;
    private final DamPermissionRepository damPermissionRepository;
    private final DocumentationPermissionService documentationPermissionService;
    private final AttributionsPermissionService attributionsPermissionService;
    private final InstrumentationPermissionService instrumentationPermissionService;
    private final RoutineInspectionPermissionService routineInspectionPermissionService;

    @PostConstruct
    public void initializeSystemUser() {
        String systemUserEmail = "noreply@geometrisa-prod.com.br";
        String systemUserName = "SISTEMA";

        try {

            Optional<UserEntity> existingUser = userRepository.findByEmail(systemUserEmail);

            if (existingUser.isPresent()) {
                log.info("Usuário do sistema já existe com ID: {}", existingUser.get().getId());
                return;
            }

            boolean systemNameExists = userRepository.existsByName(systemUserName);

            if (systemNameExists) {
                log.warn("Já existe um usuário com o nome SISTEMA. Não será criado o usuário do sistema.");
                return;
            }

            UserEntity systemUser = new UserEntity();
            systemUser.setName(systemUserName);
            systemUser.setEmail(systemUserEmail);

            systemUser.setPassword(passwordEncoder.encode("admingeom"));

            StatusEntity activeStatus = statusRepository.findByStatus(StatusEnum.ACTIVE)
                    .orElseThrow(() -> new NotFoundException("Status ACTIVE não encontrado no sistema!"));
            systemUser.setStatus(activeStatus);

            RoleEntity adminRole = roleRepository.findByName(RoleEnum.ADMIN)
                    .orElseThrow(() -> new NotFoundException("Role ADMIN não encontrada no sistema!"));
            systemUser.setRole(adminRole);

            SexEntity maleOrFirstSex = sexRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Nenhum sexo encontrado no sistema!"));
            systemUser.setSex(maleOrFirstSex);

            UserEntity savedUser = userRepository.save(systemUser);
            log.info("Usuário do sistema criado com ID: {}", savedUser.getId());

        } catch (Exception e) {
            log.error("Erro ao criar usuário do sistema: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public UserEntity updateStatus(Long userId, UserStatusUpdateDTO statusUpdateDTO) {
        UserEntity user = findEntityByIdWithAllDetails(userId);

        if (isSystemUser(user)) {
            throw new InvalidInputException("O usuário SISTEMA não pode ser modificado.");
        }

        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getAttributionsPermission().getEditUser()) {
                throw new UnauthorizedException("Usuário não tem permissão para modificar status de usuários!");
            }
        }

        StatusEntity status = statusRepository.findById(statusUpdateDTO.getStatusId())
                .orElseThrow(() -> new NotFoundException("Status não encontrado com ID: " + statusUpdateDTO.getStatusId()));

        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getAttributionsPermission().getEditUser()) {
                throw new UnauthorizedException("Usuário não tem permissão para excluir usuários!");
            }
        }

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado para exclusão!"));

        if (isSystemUser(user)) {
            throw new InvalidInputException("O usuário do sistema não pode ser excluído.");
        }

        // Verificar usuários criados por este usuário
        if (!user.getCreatedUsers().isEmpty() || !user.getReadings().isEmpty() || !user.getPsbFoldersCreated().isEmpty() || !user.getPsbFilesUploaded().isEmpty() || !user.getSharedFolders().isEmpty()) {
            throw new BusinessRuleException(
                    "Não é possível excluir o usuário, pois existem registros associados a ele, desative o usuário se necessário.");
        }

        // Se chegou até aqui, podemos remover as permissões e então o usuário
        documentationPermissionService.deleteByUserSafely(user.getId());
        attributionsPermissionService.deleteByUserSafely(user.getId());
        instrumentationPermissionService.deleteByUserSafely(user.getId());
        routineInspectionPermissionService.deleteByUserSafely(user.getId());

        List<DamPermissionEntity> damPermissions = damPermissionRepository.findByUser(user);
        if (!damPermissions.isEmpty()) {
            damPermissionRepository.deleteAll(damPermissions);
        }

        userRepository.deleteById(id);
    }

    @Transactional()
    public List<UserEntity> findByCreatedBy(Long createdById) {
        userRepository.findById(createdById)
                .orElseThrow(() -> new NotFoundException("Usuário criador não encontrado com ID: " + createdById));

        List<UserEntity> result = userRepository.findByCreatedByIdWithDetails(createdById);

        return result;
    }

    @Transactional()
    public List<UserEntity> findByFilters(Long roleId, Long clientId, Long statusId, Boolean isManagement) {
        if (isManagement != null && isManagement) {
            // Validação: clientId é obrigatório quando isManagement é true
            if (clientId == null) {
                throw new InvalidInputException("O cliente é obrigatorio!");
            }

            return userRepository.findByClientIncludingUnassignedCollaborators(clientId, statusId);
        }

        List<UserEntity> result = userRepository.findByRoleAndClientWithDetails(roleId, clientId, statusId);

        // Filtrar usuário SISTEMA do resultado
        result = result.stream()
                .filter(user -> !isSystemUser(user))
                .collect(java.util.stream.Collectors.toList());

        return result;
    }

    @Transactional
    public UserEntity save(UserCreateDTO userDTO) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getAttributionsPermission().getEditUser()) {
                throw new UnauthorizedException("Usuário não tem permissão para editar/criar usuários!");
            }
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setName(userDTO.getName());
        userEntity.setEmail(userDTO.getEmail());
        userEntity.setPhone(userDTO.getPhone());
        userEntity.setSex(userDTO.getSex());
        userEntity.setStatus(userDTO.getStatus());
        userEntity.setRole(userDTO.getRole());

        if (userDTO.getCreatedById() != null) {
            UserEntity creator = userRepository.findById(userDTO.getCreatedById())
                    .orElseThrow(() -> new NotFoundException("Usuário criador não encontrado com ID: " + userDTO.getCreatedById()));
            userEntity.setCreatedBy(creator);
        }

        if (userRepository.existsByEmail(userEntity.getEmail())) {
            throw new DuplicateResourceException("Já existe um usuário com o email informado!");
        }

        if (userEntity.getPhone() != null && !userEntity.getPhone().isEmpty() && userRepository.existsByPhone(userEntity.getPhone())) {
            throw new DuplicateResourceException("Já existe um usuário com o telefone informado!");
        }

        if (userEntity.getSex() == null || userEntity.getSex().getId() == null) {
            throw new InvalidInputException("Sexo é obrigatório!");
        }

        sexRepository.findById(userEntity.getSex().getId())
                .orElseThrow(() -> new NotFoundException("Sexo não encontrado com ID: " + userEntity.getSex().getId()));

        if (userEntity.getStatus() == null) {
            StatusEntity activeStatus = statusRepository.findByStatus(StatusEnum.ACTIVE)
                    .orElseThrow(() -> new NotFoundException("Status ACTIVE não encontrado no sistema!"));
            userEntity.setStatus(activeStatus);
        } else if (userEntity.getStatus().getId() != null) {
            StatusEntity status = statusRepository.findById(userEntity.getStatus().getId())
                    .orElseThrow(() -> new NotFoundException("Status não encontrado com ID: " + userEntity.getStatus().getId()));
            userEntity.setStatus(status);
        }

        if (userEntity.getRole() == null) {
            RoleEntity defaultRole = roleRepository.findByName(RoleEnum.COLLABORATOR)
                    .orElseThrow(() -> new NotFoundException("Role COLLABORATOR não encontrada no sistema!"));
            userEntity.setRole(defaultRole);
        } else if (userEntity.getRole().getId() != null) {
            RoleEntity role = roleRepository.findById(userEntity.getRole().getId())
                    .orElseThrow(() -> new NotFoundException("Role não encontrada com ID: " + userEntity.getRole().getId()));
            userEntity.setRole(role);
        }

        if (userEntity.getRole() != null && userEntity.getRole().getName() == RoleEnum.ADMIN) {
            userEntity.setClients(new HashSet<>());
        } else {
            if (userDTO.getClients() != null) {
                userEntity.setClients(userDTO.getClients());
            } else {
                userEntity.setClients(new HashSet<>());
            }
        }

        String generatedPassword = GenerateRandomPassword.execute();
        userEntity.setPassword(passwordEncoder.encode(generatedPassword));
        userEntity.setIsFirstAccess(true);

        UserEntity savedUser = userRepository.save(userEntity);

        emailService.sendFirstAccessPassword(savedUser.getEmail(), generatedPassword, savedUser.getName());

        if (savedUser.getRole().getName() == RoleEnum.COLLABORATOR) {
            if (userDTO.getSourceUserId() != null) {
                copyPermissionsFromUser(savedUser, userDTO.getSourceUserId());
            } else {
                if (!savedUser.getClients().isEmpty()) {
                    createDefaultDamPermissions(savedUser);
                }

                documentationPermissionService.createDefaultPermission(savedUser);
                attributionsPermissionService.createDefaultPermission(savedUser);
                instrumentationPermissionService.createDefaultPermission(savedUser);
                routineInspectionPermissionService.createDefaultPermission(savedUser);
            }
        }

        return savedUser;
    }

    @Transactional
    public UserEntity update(Long id, UserUpdateDTO userDTO) {
        UserEntity existingUser = findEntityByIdWithAllDetails(id);

        if (isSystemUser(existingUser)) {
            throw new InvalidInputException("O usuário SISTEMA não pode ser modificado.");
        }

        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();

            if (userLogged.getRole() != null
                    && userLogged.getRole().getName() == RoleEnum.COLLABORATOR
                    && existingUser.getRole() != null
                    && existingUser.getRole().getName() == RoleEnum.ADMIN) {
                throw new UnauthorizedException("Colaborador não pode editar usuário administrador.");
            }

            if (userLogged.getId() != id.longValue()) {
                if (!userLogged.getAttributionsPermission().getEditUser()) {
                    throw new UnauthorizedException("Usuário não tem permissão para editar usuários que não sejam ele mesmo!");
                }
            }
        }

        if (userRepository.existsByEmailAndIdNot(userDTO.getEmail(), id)) {
            throw new DuplicateResourceException("Já existe um usuário com o email informado!");
        }

        if (userDTO.getPhone() != null && !userDTO.getPhone().isEmpty()) {
            if (userRepository.existsByPhoneAndIdNot(userDTO.getPhone(), id)) {
                throw new DuplicateResourceException("Já existe um usuário com o telefone informado!");
            }
        }

        if (userDTO.getSex() == null || userDTO.getSex().getId() == null) {
            throw new InvalidInputException("Sexo é obrigatório!");
        }

        sexRepository.findById(userDTO.getSex().getId())
                .orElseThrow(() -> new NotFoundException("Sexo não encontrado com ID: " + userDTO.getSex().getId()));

        if (userDTO.getStatus() != null && userDTO.getStatus().getId() != null) {
            StatusEntity status = statusRepository.findById(userDTO.getStatus().getId())
                    .orElseThrow(() -> new NotFoundException("Status não encontrado com ID: " + userDTO.getStatus().getId()));
            existingUser.setStatus(status);
        }

        boolean roleChanged = false;
        RoleEnum oldRole = existingUser.getRole().getName();
        RoleEnum newRole = oldRole;

        if (userDTO.getRole() != null && userDTO.getRole().getId() != null) {
            RoleEntity role = roleRepository.findById(userDTO.getRole().getId())
                    .orElseThrow(() -> new NotFoundException("Role não encontrada com ID: " + userDTO.getRole().getId()));

            if (!existingUser.getRole().equals(role)) {
                roleChanged = true;
                newRole = role.getName();
            }

            existingUser.setRole(role);
        }

        existingUser.setName(userDTO.getName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setPhone(userDTO.getPhone());
        existingUser.setSex(userDTO.getSex());

        UserEntity savedUser = userRepository.save(existingUser);

        if (roleChanged) {
            handleRoleChange(savedUser, oldRole, newRole);
        }

        return savedUser;
    }

    @Transactional
    public UserEntity updatePassword(Long id, UserPasswordUpdateDTO passwordDTO) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (userLogged.getId() != id.longValue()) {
                if (!userLogged.getAttributionsPermission().getEditUser()) {
                    throw new UnauthorizedException("Usuário não tem permissão para atualizar a senha de outros usuários!");
                }
            }
        }

        UserEntity existingUser = findEntityByIdWithAllDetails(id);

        if (!passwordEncoder.matches(passwordDTO.getCurrentPassword(), existingUser.getPassword())) {
            throw new InvalidInputException("Senha atual incorreta!");
        }

        if (passwordDTO.getCurrentPassword().equals(passwordDTO.getNewPassword())) {
            throw new InvalidInputException("A nova senha deve ser diferente da senha atual!");
        }

        existingUser.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));

        if (existingUser.getIsFirstAccess()) {
            existingUser.setIsFirstAccess(false);
        }

        UserEntity savedUser = userRepository.save(existingUser);

        return savedUser;
    }

    @Transactional
    public UserEntity updateUserClients(Long userId, UserClientAssociationDTO clientAssociationDTO) {
        AuthenticatedUserUtil.checkAdminPermission();
        UserEntity user = findEntityByIdWithAllDetails(userId);

        // Se o usuário alvo for ADMIN, ignorar associação de clients
        if (user.getRole() != null && user.getRole().getName() == RoleEnum.ADMIN) {
            // Não altera clients de admin — retorna sem modificações
            return user;
        }

        Set<ClientEntity> oldClients = new HashSet<>(user.getClients());

        Set<ClientEntity> newClients = new HashSet<>();

        for (Long clientId : clientAssociationDTO.getClientIds()) {
            ClientEntity client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new NotFoundException("Cliente com ID " + clientId + " não encontrado!"));
            newClients.add(client);
        }

        user.setClients(newClients);

        if (user.getRole() != null && user.getRole().getName() == RoleEnum.COLLABORATOR) {
            Set<ClientEntity> addedClients = new HashSet<>(newClients);
            addedClients.removeAll(oldClients);

            if (!addedClients.isEmpty()) {
                createDamPermissionsForSpecificClients(user, addedClients);
            }

            Set<ClientEntity> removedClients = new HashSet<>(oldClients);
            removedClients.removeAll(newClients);

            if (!removedClients.isEmpty()) {
                deleteDamPermissionsForSpecificClients(user, removedClients);
            }
        }

        UserEntity savedUser = userRepository.save(user);

        return savedUser;
    }

    @Transactional()
    public UserEntity findById(Long id) {
        UserEntity user = findEntityByIdWithAllDetails(id);
        return user;
    }

    @Transactional()
    public UserEntity findByEmail(String email) {
        UserEntity user = userRepository.findByEmailWithBasicDetails(email)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com email: " + email));

        return user;
    }

    @Transactional()
    public List<UserEntity> findAll() {
        List<UserEntity> users = userRepository.findAllWithBasicDetails();

        users.forEach(user -> {
            if (!Hibernate.isInitialized(user.getClients())) {
                user.getClients().size();
            }
        });
        return users;
    }

    @Transactional()
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional()
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return userRepository.existsByEmailAndIdNot(email, id);
    }

    private UserEntity findEntityByIdWithAllDetails(Long id) {
        UserEntity user = userRepository.findByIdWithAllDetails(id)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        if (!Hibernate.isInitialized(user.getClients())) {
            user.getClients().size();
        }

        return user;
    }

    @Transactional
    public Object initiateLogin(LoginRequestDTO userDTO) {
        UserEntity user = userRepository.findByEmailWithClientsAndDetails(userDTO.email())
                .orElseThrow(() -> new NotFoundException("Credenciais incorretas!"));

        if (user.getStatus().getStatus() == StatusEnum.DISABLED) {
            throw new UnauthorizedException("Usuário não tem acesso ao sistema!");
        }

        if (!passwordEncoder.matches(userDTO.password(), user.getPassword())) {
            throw new InvalidInputException("Credenciais incorretas!");
        }

        if (user.getLastToken() != null && user.getTokenExpiryDate() != null
                && LocalDateTime.now().isBefore(user.getTokenExpiryDate())
                && tokenService.isTokenValid(user.getLastToken())) {

            List<ClientEntity> clients = new ArrayList<>(user.getClients());

            return new LoginResponseDTO(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getSex(),
                    user.getRole().getName(),
                    user.getIsFirstAccess(),
                    user.getLastToken(),
                    clients
            );
        }

        if (isSystemUser(user)) {
            String token = tokenService.generateToken(user);

            user.setLastToken(token);
            user.setTokenExpiryDate(LocalDateTime.now().plusHours(12));
            userRepository.save(user);

            List<ClientEntity> clients = new ArrayList<>(user.getClients());

            return new LoginResponseDTO(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getSex(),
                    user.getRole().getName(),
                    user.getIsFirstAccess(),
                    token,
                    clients
            );
        }

        String verificationCode = GenerateRandomCode.generateRandomCode();

        VerificationCodeEntity codeEntity = new VerificationCodeEntity();
        codeEntity.setCode(verificationCode);
        codeEntity.setUser(user);
        codeEntity.setUsed(false);
        codeEntity.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        verificationCodeRepository.save(codeEntity);

        emailService.sendVerificationCode(user.getEmail(), verificationCode);

        return null;
    }

    @Transactional
    public LoginResponseDTO verifyCodeAndLogin(VerifyCodeRequestDTO verifyRequest) {
        UserEntity user = userRepository.findByEmailWithClientsAndDetails(verifyRequest.getEmail())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        VerificationCodeEntity codeEntity = verificationCodeRepository
                .findLatestActiveByUser(user)
                .orElseThrow(() -> new NotFoundException("Código de verificação não encontrado ou expirado!"));

        if (!codeEntity.getCode().equals(verifyRequest.getCode())) {
            throw new InvalidInputException("Código de verificação inválido!");
        }

        if (LocalDateTime.now().isAfter(codeEntity.getExpiryDate())) {
            throw new InvalidInputException("Código de verificação expirado!");
        }

        codeEntity.setUsed(true);
        verificationCodeRepository.save(codeEntity);

        String token = tokenService.generateToken(user);

        userRepository.save(user);

        List<ClientEntity> clients = new ArrayList<>(user.getClients());

        return new LoginResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getSex(),
                user.getRole().getName(),
                user.getIsFirstAccess(),
                token,
                clients
        );
    }

    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequestDTO requestDTO) {
        UserEntity user = userRepository.findByEmail(requestDTO.getEmail())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com este email!"));

        List<VerificationCodeEntity> activeCodes = verificationCodeRepository
                .findAllByUserAndUsedFalseOrderByExpiryDateDesc(user);

        for (VerificationCodeEntity code : activeCodes) {
            code.setUsed(true);
            verificationCodeRepository.save(code);
        }

        String verificationCode = GenerateRandomCode.generateRandomCode();

        VerificationCodeEntity codeEntity = new VerificationCodeEntity();
        codeEntity.setCode(verificationCode);
        codeEntity.setUser(user);
        codeEntity.setUsed(false);
        codeEntity.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        verificationCodeRepository.save(codeEntity);

        emailService.sendPasswordResetCode(user.getEmail(), verificationCode);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDTO requestDTO) {
        UserEntity user = userRepository.findByEmail(requestDTO.getEmail())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com este email!"));

        VerificationCodeEntity codeEntity = verificationCodeRepository
                .findLatestActiveByUser(user)
                .orElseThrow(() -> new NotFoundException("Código de verificação não encontrado ou expirado!"));

        if (!codeEntity.getCode().equals(requestDTO.getCode())) {
            throw new InvalidInputException("Código de verificação inválido!");
        }

        if (LocalDateTime.now().isAfter(codeEntity.getExpiryDate())) {
            throw new InvalidInputException("Código de verificação expirado!");
        }

        codeEntity.setUsed(true);
        verificationCodeRepository.save(codeEntity);

        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public boolean verifyResetCode(VerifyCodeRequestDTO verifyRequest) {
        UserEntity user = userRepository.findByEmail(verifyRequest.getEmail())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        VerificationCodeEntity codeEntity = verificationCodeRepository
                .findLatestActiveByUser(user)
                .orElseThrow(() -> new NotFoundException("Código de verificação não encontrado ou expirado!"));

        if (!codeEntity.getCode().equals(verifyRequest.getCode())) {
            throw new InvalidInputException("Código de verificação inválido!");
        }

        if (LocalDateTime.now().isAfter(codeEntity.getExpiryDate())) {
            throw new InvalidInputException("Código de verificação expirado!");
        }

        return true;
    }

    private void handleRoleChange(UserEntity user, RoleEnum oldRole, RoleEnum newRole) {
        if (oldRole == RoleEnum.ADMIN && newRole == RoleEnum.COLLABORATOR) {

            if (!user.getClients().isEmpty()) {
                createDefaultDamPermissions(user);
            }

            documentationPermissionService.createDefaultPermission(user);
            attributionsPermissionService.createDefaultPermission(user);
            instrumentationPermissionService.createDefaultPermission(user);
            routineInspectionPermissionService.createDefaultPermission(user);
        }

        if (oldRole == RoleEnum.COLLABORATOR && newRole == RoleEnum.ADMIN) {

            deleteAllDamPermissions(user);

            documentationPermissionService.deleteByUserSafely(user.getId());
            attributionsPermissionService.deleteByUserSafely(user.getId());
            instrumentationPermissionService.deleteByUserSafely(user.getId());
            routineInspectionPermissionService.deleteByUserSafely(user.getId());

            if (!user.getClients().isEmpty()) {
                user.getClients().clear();
                userRepository.save(user);
            }
        }
    }

    private void createDefaultDamPermissions(UserEntity user) {

        for (ClientEntity client : user.getClients()) {
            List<DamEntity> dams = damRepository.findByClient(client);

            for (DamEntity dam : dams) {
                if (damPermissionRepository.existsByUserAndDamAndClient(user, dam, client)) {
                    continue;
                }

                DamPermissionEntity permission = new DamPermissionEntity();
                permission.setUser(user);
                permission.setDam(dam);
                permission.setClient(client);
                permission.setHasAccess(false);
                permission.setCreatedAt(LocalDateTime.now());

                damPermissionRepository.save(permission);
            }
        }
    }

    private void deleteAllDamPermissions(UserEntity user) {
        List<DamPermissionEntity> permissions = damPermissionRepository.findByUser(user);

        if (!permissions.isEmpty()) {
            damPermissionRepository.deleteAll(permissions);
        }
    }

    private void createDamPermissionsForSpecificClients(UserEntity user, Set<ClientEntity> clients) {
        for (ClientEntity client : clients) {
            List<DamEntity> dams = damRepository.findByClient(client);

            for (DamEntity dam : dams) {
                if (damPermissionRepository.existsByUserAndDamAndClient(user, dam, client)) {
                    continue;
                }

                DamPermissionEntity permission = new DamPermissionEntity();
                permission.setUser(user);
                permission.setDam(dam);
                permission.setClient(client);
                permission.setHasAccess(false);
                permission.setCreatedAt(LocalDateTime.now());

                damPermissionRepository.save(permission);
            }
        }
    }

    private void deleteDamPermissionsForSpecificClients(UserEntity user, Set<ClientEntity> clients) {
        for (ClientEntity client : clients) {
            List<DamPermissionEntity> permissions = damPermissionRepository.findByUserAndClient(user, client);
            if (!permissions.isEmpty()) {
                damPermissionRepository.deleteAll(permissions);
            }
        }
    }

    @Transactional
    private void copyPermissionsFromUser(UserEntity targetUser, Long sourceUserId) {
        UserEntity sourceUser = userRepository.findById(sourceUserId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado para cópia da permissões!"));

        if (sourceUser.getRole().getName() != RoleEnum.COLLABORATOR) {
            throw new InvalidInputException("O usuário fonte deve ser um colaborador para copiar permissões!");
        }

        try {
            var sourceDocPermission = documentationPermissionService.findByUser(sourceUser.getId());
            var docPermissionDTO = new DocumentationPermissionDTO();
            docPermissionDTO.setUserId(targetUser.getId());
            docPermissionDTO.setViewPSB(sourceDocPermission.getViewPSB());
            docPermissionDTO.setEditPSB(sourceDocPermission.getEditPSB());
            docPermissionDTO.setSharePSB(sourceDocPermission.getSharePSB());
            documentationPermissionService.createOrUpdate(docPermissionDTO);
        } catch (NotFoundException e) {
            documentationPermissionService.createDefaultPermission(targetUser);
        }

        try {
            var sourceAttrPermission = attributionsPermissionService.findByUser(sourceUser.getId());
            var attrPermissionDTO = new AttributionsPermissionDTO();
            attrPermissionDTO.setUserId(targetUser.getId());
            attrPermissionDTO.setEditUser(sourceAttrPermission.getEditUser());
            attrPermissionDTO.setEditDam(sourceAttrPermission.getEditDam());
            attrPermissionDTO.setEditGeralData(sourceAttrPermission.getEditGeralData());
            attributionsPermissionService.createOrUpdate(attrPermissionDTO);
        } catch (NotFoundException e) {
            attributionsPermissionService.createDefaultPermission(targetUser);
        }

        try {
            var sourceInstrPermission = instrumentationPermissionService.findByUser(sourceUser.getId());
            var instrPermissionDTO = new InstrumentationPermissionDTO();
            instrPermissionDTO.setUserId(targetUser.getId());
            instrPermissionDTO.setViewGraphs(sourceInstrPermission.getViewGraphs());
            instrPermissionDTO.setEditGraphsLocal(sourceInstrPermission.getEditGraphsLocal());
            instrPermissionDTO.setEditGraphsDefault(sourceInstrPermission.getEditGraphsDefault());
            instrPermissionDTO.setViewRead(sourceInstrPermission.getViewRead());
            instrPermissionDTO.setEditRead(sourceInstrPermission.getEditRead());
            instrPermissionDTO.setViewSections(sourceInstrPermission.getViewSections());
            instrPermissionDTO.setEditSections(sourceInstrPermission.getEditSections());
            instrumentationPermissionService.createOrUpdate(instrPermissionDTO);
        } catch (NotFoundException e) {
            instrumentationPermissionService.createDefaultPermission(targetUser);
        }

        try {
            var sourceRoutinePermission = routineInspectionPermissionService.findByUser(sourceUser.getId());
            var routinePermissionDTO = new RoutineInspectionPermissionDTO();
            routinePermissionDTO.setUserId(targetUser.getId());
            routinePermissionDTO.setIsFillWeb(sourceRoutinePermission.getIsFillWeb());
            routinePermissionDTO.setIsFillMobile(sourceRoutinePermission.getIsFillMobile());
            routineInspectionPermissionService.createOrUpdate(routinePermissionDTO);
        } catch (NotFoundException e) {
            routineInspectionPermissionService.createDefaultPermission(targetUser);
        }

        if (!targetUser.getClients().isEmpty()) {
            createDefaultDamPermissions(targetUser);
        }
    }

    public boolean isSystemUser(UserEntity user) {
        return user != null
                && "SISTEMA".equals(user.getName())
                && "noreply@geometrisa-prod.com.br".equals(user.getEmail());
    }

    /**
     * Verifica se o ID pertence ao usuário do sistema.
     */
    public boolean isSystemUser(Long userId) {
        try {
            UserEntity user = findById(userId);
            return isSystemUser(user);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retorna o ID do usuário do sistema.
     */
    public Long getSystemUserId() {
        String systemUserEmail = "noreply@geometrisa-prod.com.br";
        UserEntity systemUser = userRepository.findByEmail(systemUserEmail)
                .orElseThrow(() -> new NotFoundException("Usuário do sistema não encontrado!"));
        return systemUser.getId();
    }

    /**
     * Retorna o usuário do sistema.
     */
    public UserEntity getSystemUser() {
        String systemUserEmail = "noreply@geometrisa-prod.com.br";
        return userRepository.findByEmail(systemUserEmail)
                .orElseThrow(() -> new NotFoundException("Usuário do sistema não encontrado!"));
    }
}
