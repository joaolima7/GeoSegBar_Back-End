package com.geosegbar.infra.verification_code.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.UserEntity;
import com.geosegbar.entities.VerificationCodeEntity;

import jakarta.transaction.Transactional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCodeEntity, Long>{
    @Query("SELECT v FROM VerificationCodeEntity v WHERE v.user = :user AND v.used = false " +
           "ORDER BY v.expiryDate DESC LIMIT 1")
    Optional<VerificationCodeEntity> findLatestActiveByUser(@Param("user") UserEntity user);
    
    Optional<VerificationCodeEntity> findByCodeAndUsedFalse(String code);
    List<VerificationCodeEntity> findAllByUserAndUsedFalseOrderByExpiryDateDesc(UserEntity user);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationCodeEntity v WHERE v.used = true OR v.expiryDate < :now")
    int deleteAllUsedOrExpired(@Param("now") LocalDateTime now);
}
