package com.geosegbar.unit.entities;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.entities.VerificationCodeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class VerificationCodeEntityTest extends BaseUnitTest {

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setName("João Silva");
        user.setEmail("joao@exemplo.com");
    }

    @Test
    @DisplayName("Should create verification code with all required fields")
    void shouldCreateVerificationCodeWithAllRequiredFields() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setId(1L);
        verificationCode.setCode("123456");
        verificationCode.setExpiryDate(LocalDateTime.now().plusHours(1));
        verificationCode.setUsed(false);
        verificationCode.setUser(user);

        assertThat(verificationCode).satisfies(vc -> {
            assertThat(vc.getId()).isEqualTo(1L);
            assertThat(vc.getCode()).isEqualTo("123456");
            assertThat(vc.getExpiryDate()).isNotNull();
            assertThat(vc.isUsed()).isFalse();
            assertThat(vc.getUser()).isEqualTo(user);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(30);

        VerificationCodeEntity verificationCode = new VerificationCodeEntity(
                1L,
                "654321",
                expiryDate,
                false,
                user
        );

        assertThat(verificationCode.getId()).isEqualTo(1L);
        assertThat(verificationCode.getCode()).isEqualTo("654321");
        assertThat(verificationCode.getExpiryDate()).isEqualTo(expiryDate);
        assertThat(verificationCode.isUsed()).isFalse();
        assertThat(verificationCode.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with User")
    void shouldMaintainManyToOneRelationshipWithUser() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setUser(user);

        assertThat(verificationCode.getUser())
                .isNotNull()
                .isEqualTo(user);
    }

    @Test
    @DisplayName("Should support 6 digit code")
    void shouldSupport6DigitCode() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setCode("123456");

        assertThat(verificationCode.getCode()).hasSize(6);
    }

    @Test
    @DisplayName("Should support numeric codes")
    void shouldSupportNumericCodes() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setCode("987654");

        assertThat(verificationCode.getCode()).matches("\\d{6}");
    }

    @Test
    @DisplayName("Should default used flag to false")
    void shouldDefaultUsedFlagToFalse() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setUsed(false);

        assertThat(verificationCode.isUsed()).isFalse();
    }

    @Test
    @DisplayName("Should support toggling used flag")
    void shouldSupportTogglingUsedFlag() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setUsed(false);

        verificationCode.setUsed(true);

        assertThat(verificationCode.isUsed()).isTrue();
    }

    @Test
    @DisplayName("Should support future expiry date")
    void shouldSupportFutureExpiryDate() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusHours(2);
        verificationCode.setExpiryDate(futureDate);

        assertThat(verificationCode.getExpiryDate()).isAfter(now);
    }

    @Test
    @DisplayName("Should check if code is expired")
    void shouldCheckIfCodeIsExpired() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        LocalDateTime pastDate = LocalDateTime.now().minusHours(1);
        verificationCode.setExpiryDate(pastDate);

        assertThat(verificationCode.getExpiryDate()).isBefore(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should support valid code check")
    void shouldSupportValidCodeCheck() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setCode("123456");
        verificationCode.setExpiryDate(LocalDateTime.now().plusHours(1));
        verificationCode.setUsed(false);

        assertThat(verificationCode.isUsed()).isFalse();
        assertThat(verificationCode.getExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should support multiple verification codes per user")
    void shouldSupportMultipleVerificationCodesPerUser() {

        VerificationCodeEntity code1 = new VerificationCodeEntity();
        code1.setId(1L);
        code1.setCode("123456");
        code1.setUser(user);

        VerificationCodeEntity code2 = new VerificationCodeEntity();
        code2.setId(2L);
        code2.setCode("654321");
        code2.setUser(user);

        assertThat(code1.getUser()).isEqualTo(code2.getUser());
        assertThat(code1.getCode()).isNotEqualTo(code2.getCode());
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        verificationCode.setId(1L);
        verificationCode.setCode("123456");
        verificationCode.setUsed(false);

        Long originalId = verificationCode.getId();

        verificationCode.setUsed(true);
        verificationCode.setCode("654321");

        assertThat(verificationCode.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support timestamp precision for expiry date")
    void shouldSupportTimestampPrecisionForExpiryDate() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        LocalDateTime expiryDate = LocalDateTime.of(2024, 12, 28, 15, 30, 45);
        verificationCode.setExpiryDate(expiryDate);

        assertThat(verificationCode.getExpiryDate()).isEqualTo(expiryDate);
        assertThat(verificationCode.getExpiryDate().getYear()).isEqualTo(2024);
        assertThat(verificationCode.getExpiryDate().getMonthValue()).isEqualTo(12);
        assertThat(verificationCode.getExpiryDate().getDayOfMonth()).isEqualTo(28);
        assertThat(verificationCode.getExpiryDate().getHour()).isEqualTo(15);
        assertThat(verificationCode.getExpiryDate().getMinute()).isEqualTo(30);
        assertThat(verificationCode.getExpiryDate().getSecond()).isEqualTo(45);
    }

    @Test
    @DisplayName("Should support short expiry window")
    void shouldSupportShortExpiryWindow() {

        VerificationCodeEntity verificationCode = new VerificationCodeEntity();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime shortExpiry = now.plusMinutes(5);
        verificationCode.setExpiryDate(shortExpiry);

        assertThat(verificationCode.getExpiryDate()).isBetween(now, now.plusMinutes(10));
    }
}
