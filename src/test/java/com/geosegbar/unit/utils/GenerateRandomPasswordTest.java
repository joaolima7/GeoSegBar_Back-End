package com.geosegbar.unit.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.utils.GenerateRandomPassword;
import com.geosegbar.config.BaseUnitTest;

@Tag("unit")
class GenerateRandomPasswordTest extends BaseUnitTest {

    @Test
    @DisplayName("Should generate 8 character password")
    void shouldGenerate8CharacterPassword() {
        // When
        String password = GenerateRandomPassword.execute();

        // Then
        assertThat(password).hasSize(8);
    }

    @Test
    @DisplayName("Should generate non null password")
    void shouldGenerateNonNullPassword() {
        // When
        String password = GenerateRandomPassword.execute();

        // Then
        assertThat(password).isNotNull();
    }

    @Test
    @DisplayName("Should generate non empty password")
    void shouldGenerateNonEmptyPassword() {
        // When
        String password = GenerateRandomPassword.execute();

        // Then
        assertThat(password).isNotEmpty();
    }

    @Test
    @DisplayName("Should generate password containing uppercase letter")
    void shouldGeneratePasswordContainingUppercaseLetter() {
        // When
        String password = GenerateRandomPassword.execute();

        // Then
        assertThat(password).matches(".*[A-Z].*");
    }

    @Test
    @DisplayName("Should generate password containing lowercase letter")
    void shouldGeneratePasswordContainingLowercaseLetter() {
        // When
        String password = GenerateRandomPassword.execute();

        // Then
        assertThat(password).matches(".*[a-z].*");
    }

    @Test
    @DisplayName("Should generate password containing number")
    void shouldGeneratePasswordContainingNumber() {
        // When
        String password = GenerateRandomPassword.execute();

        // Then
        assertThat(password).matches(".*\\d.*");
    }

    @Test
    @DisplayName("Should generate password containing special character")
    void shouldGeneratePasswordContainingSpecialCharacter() {
        // When
        String password = GenerateRandomPassword.execute();

        // Then
        assertThat(password).matches(".*[!@#$%^&*()\\-_=+].*");
    }

    @Test
    @DisplayName("Should generate password with all required character types")
    void shouldGeneratePasswordWithAllRequiredCharacterTypes() {
        // When
        String password = GenerateRandomPassword.execute();

        // Then
        assertThat(password).matches(".*[A-Z].*"); // uppercase
        assertThat(password).matches(".*[a-z].*"); // lowercase
        assertThat(password).matches(".*\\d.*");    // number
        assertThat(password).matches(".*[!@#$%^&*()\\-_=+].*"); // special char
    }

    @Test
    @DisplayName("Should generate password with only allowed characters")
    void shouldGeneratePasswordWithOnlyAllowedCharacters() {
        // When
        String password = GenerateRandomPassword.execute();

        // Then
        String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        for (char c : password.toCharArray()) {
            assertThat(allowedChars).contains(String.valueOf(c));
        }
    }

    @Test
    @DisplayName("Should generate different passwords in multiple calls")
    void shouldGenerateDifferentPasswordsInMultipleCalls() {
        // Given
        Set<String> passwords = new HashSet<>();
        int iterations = 100;

        // When
        for (int i = 0; i < iterations; i++) {
            passwords.add(GenerateRandomPassword.execute());
        }

        // Then
        assertThat(passwords.size()).isGreaterThan(iterations / 2); // At least 50% unique
    }

    @Test
    @DisplayName("Should generate multiple consecutive passwords that are distinct")
    void shouldGenerateMultipleConsecutivePasswordsThatAreDistinct() {
        // When
        String password1 = GenerateRandomPassword.execute();
        String password2 = GenerateRandomPassword.execute();
        String password3 = GenerateRandomPassword.execute();

        // Then
        assertThat(password1).isNotEqualTo(password2);
        assertThat(password2).isNotEqualTo(password3);
        assertThat(password1).isNotEqualTo(password3);
    }

    @Test
    @DisplayName("Should generate password as string type")
    void shouldGeneratePasswordAsStringType() {
        // When
        String password = GenerateRandomPassword.execute();

        // Then
        assertThat(password).isInstanceOf(String.class);
    }
}
