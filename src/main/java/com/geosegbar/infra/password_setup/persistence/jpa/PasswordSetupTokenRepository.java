package com.geosegbar.infra.password_setup.persistence.jpa;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.geosegbar.entities.PasswordSetupTokenEntity;

@Repository
public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupTokenEntity, Long> {

    @Query("SELECT t FROM PasswordSetupTokenEntity t JOIN FETCH t.user WHERE t.token = :token")
    Optional<PasswordSetupTokenEntity> findByTokenWithUser(@Param("token") String token);

    /**
     * Marca como usados todos os tokens pendentes de um usuário. Chamado sempre
     * que um novo link é emitido e quando a senha é definida, para que só exista
     * um link válido por vez.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE PasswordSetupTokenEntity t SET t.used = true, t.usedAt = :now "
            + "WHERE t.user.id = :userId AND t.used = false")
    int invalidatePendingTokens(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordSetupTokenEntity t WHERE t.used = true OR t.expiryDate < :now")
    int deleteAllUsedOrExpired(@Param("now") LocalDateTime now);
}
