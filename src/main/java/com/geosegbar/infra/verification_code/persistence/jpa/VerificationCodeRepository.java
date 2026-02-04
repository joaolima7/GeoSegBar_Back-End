package com.geosegbar.infra.verification_code.persistence.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.UserEntity;
import com.geosegbar.entities.VerificationCodeEntity;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCodeEntity, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<VerificationCodeEntity> findFirstByUserAndUsedFalseOrderByExpiryDateDesc(UserEntity user);

    default Optional<VerificationCodeEntity> findLatestActiveByUser(UserEntity user) {
        return findFirstByUserAndUsedFalseOrderByExpiryDateDesc(user);
    }

    @EntityGraph(attributePaths = {"user"})
    Optional<VerificationCodeEntity> findByCodeAndUsedFalse(String code);

    @EntityGraph(attributePaths = {"user"})
    List<VerificationCodeEntity> findAllByUserAndUsedFalseOrderByExpiryDateDesc(UserEntity user);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationCodeEntity v WHERE v.used = true OR v.expiryDate < :now")
    int deleteAllUsedOrExpired(@Param("now") LocalDateTime now);
}
