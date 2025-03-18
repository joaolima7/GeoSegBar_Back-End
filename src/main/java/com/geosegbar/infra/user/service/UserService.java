package com.geosegbar.infra.user.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.geosegbar.common.email.EmailService;
import com.geosegbar.common.utils.GenerateRandomCode;
import com.geosegbar.configs.security.TokenService;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.entities.VerificationCodeEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.user.dto.LoginRequestDTO;
import com.geosegbar.infra.user.dto.LoginResponseDTO;
import com.geosegbar.infra.user.dto.UserClientAssociationDTO;
import com.geosegbar.infra.user.dto.UserPasswordUpdateDTO;
import com.geosegbar.infra.user.dto.UserUpdateDTO;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;
import com.geosegbar.infra.verification_code.dto.ForgotPasswordRequestDTO;
import com.geosegbar.infra.verification_code.dto.ResetPasswordRequestDTO;
import com.geosegbar.infra.verification_code.dto.VerifyCodeRequestDTO;
import com.geosegbar.infra.verification_code.persistence.jpa.VerificationCodeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;

    @Transactional
    public void deleteById(Long id) {
        userRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Usuário não encontrado para exclusão!"));

        userRepository.deleteById(id);
    }

    @Transactional
    public UserEntity save(UserEntity userEntity) {
        if(userRepository.existsByEmail(userEntity.getEmail())){
            throw new DuplicateResourceException("Já existe um usuário com o email informado!");
        }
        
        userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
        return userRepository.save(userEntity);
    }

    @Transactional
    public UserEntity update(Long id, UserUpdateDTO userDTO) {
        UserEntity existingUser = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado para atualização!"));

        if(userRepository.existsByEmailAndIdNot(userDTO.getEmail(), id)) {
            throw new DuplicateResourceException("Já existe um usuário com o email informado!");
        }

        existingUser.setName(userDTO.getName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setPhone(userDTO.getPhone());
        existingUser.setSex(userDTO.getSex());

        return userRepository.save(existingUser);
    }

    @Transactional
    public UserEntity updatePassword(Long id, UserPasswordUpdateDTO passwordDTO) {
        UserEntity existingUser = userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado para atualização de senha!"));
            
        if (!passwordEncoder.matches(passwordDTO.getCurrentPassword(), existingUser.getPassword())) {
            throw new InvalidInputException("Senha atual incorreta!");
        }
        
        if (passwordDTO.getCurrentPassword().equals(passwordDTO.getNewPassword())) {
            throw new InvalidInputException("A nova senha deve ser diferente da senha atual!");
        }
        
        existingUser.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        return userRepository.save(existingUser);
    }

    @Transactional
    public UserEntity updateUserClients(Long userId, UserClientAssociationDTO clientAssociationDTO) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuário não encontrado para atualização de clientes!"));
        
        Set<ClientEntity> clients = new HashSet<>();
        
        for (Long clientId : clientAssociationDTO.getClientIds()) {
            ClientEntity client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Cliente com ID " + clientId + " não encontrado!"));
            clients.add(client);
        }
        
        user.setClients(clients);
        
        return userRepository.save(user);
    }

    public UserEntity findById(Long id) {
        return userRepository.findById(id).
        orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
    }


    @Transactional
    public void initiateLogin(LoginRequestDTO userDTO) {
        UserEntity user = userRepository.findByEmail(userDTO.email())
            .orElseThrow(() -> new NotFoundException("Credenciais incorretas!"));

        if (!passwordEncoder.matches(userDTO.password(), user.getPassword())) {
            throw new InvalidInputException("Credenciais incorretas!");
        }
        
        String verificationCode = GenerateRandomCode.generateRandomCode();
        
        VerificationCodeEntity codeEntity = new VerificationCodeEntity();
        codeEntity.setCode(verificationCode);
        codeEntity.setUser(user);
        codeEntity.setUsed(false);
        codeEntity.setExpiryDate(LocalDateTime.now().plusMinutes(10)); 
        
        verificationCodeRepository.save(codeEntity);
        
        emailService.sendVerificationCode(user.getEmail(), verificationCode);
    }


    @Transactional
    public LoginResponseDTO verifyCodeAndLogin(VerifyCodeRequestDTO verifyRequest) {
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
        
        codeEntity.setUsed(true);
        verificationCodeRepository.save(codeEntity);
        
        String token = tokenService.generateToken(user);
        return new LoginResponseDTO(
            user.getId(), 
            user.getName(), 
            user.getEmail(), 
            user.getPhone(), 
            user.getSex(), 
            token
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

    public List<UserEntity> findAll() {
        return userRepository.findAllByOrderByIdAsc();
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByEmailAndIdNot(String email, Long id) {
        return userRepository.existsByEmailAndIdNot(email, id);
    }
}
