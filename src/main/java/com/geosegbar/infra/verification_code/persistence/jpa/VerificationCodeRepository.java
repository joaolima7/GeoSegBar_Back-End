package com.geosegbar.infra.verification_code.persistence.jpa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.UserEntity;
import com.geosegbar.entities.VerificationCodeEntity;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCodeEntity, Long>{
    Optional<VerificationCodeEntity> findByUserAndUsedFalseOrderByExpiryDateDesc(UserEntity user);
    Optional<VerificationCodeEntity> findByCodeAndUsedFalse(String code);
}
