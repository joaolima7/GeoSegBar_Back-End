package com.geosegbar.unit.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.utils.DateFormatter;
import com.geosegbar.config.BaseUnitTest;

@Tag("unit")
class DateFormatterTest extends BaseUnitTest {

    @Test
    @DisplayName("Should format date time with correct pattern dd/MM/yyyy HH:mm:ss")
    void shouldFormatDateTimeWithCorrectPattern() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 28, 14, 30, 45);

        // When
        String formatted = DateFormatter.formatDateTime(dateTime);

        // Then
        assertThat(formatted).isEqualTo("28/12/2024 14:30:45");
    }

    @Test
    @DisplayName("Should format date time at start of year")
    void shouldFormatDateTimeAtStartOfYear() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);

        // When
        String formatted = DateFormatter.formatDateTime(dateTime);

        // Then
        assertThat(formatted).isEqualTo("01/01/2024 00:00:00");
    }

    @Test
    @DisplayName("Should format date time at end of year")
    void shouldFormatDateTimeAtEndOfYear() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 12, 31, 23, 59, 59);

        // When
        String formatted = DateFormatter.formatDateTime(dateTime);

        // Then
        assertThat(formatted).isEqualTo("31/12/2024 23:59:59");
    }

    @Test
    @DisplayName("Should format date time with midnight hour")
    void shouldFormatDateTimeWithMidnightHour() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 0, 0, 0);

        // When
        String formatted = DateFormatter.formatDateTime(dateTime);

        // Then
        assertThat(formatted).isEqualTo("15/06/2024 00:00:00");
        assertThat(formatted).contains("00:00:00");
    }

    @Test
    @DisplayName("Should format date time with noon hour")
    void shouldFormatDateTimeWithNoonHour() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 12, 30, 45);

        // When
        String formatted = DateFormatter.formatDateTime(dateTime);

        // Then
        assertThat(formatted).isEqualTo("15/06/2024 12:30:45");
        assertThat(formatted).contains("12:30:45");
    }

    @Test
    @DisplayName("Should format date time with last hour of day")
    void shouldFormatDateTimeWithLastHourOfDay() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 23, 59, 59);

        // When
        String formatted = DateFormatter.formatDateTime(dateTime);

        // Then
        assertThat(formatted).isEqualTo("15/06/2024 23:59:59");
        assertThat(formatted).contains("23:59:59");
    }

    @Test
    @DisplayName("Should format date time on leap year")
    void shouldFormatDateTimeOnLeapYear() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 2, 29, 10, 15, 30);

        // When
        String formatted = DateFormatter.formatDateTime(dateTime);

        // Then
        assertThat(formatted).isEqualTo("29/02/2024 10:15:30");
        assertThat(formatted).contains("29/02/2024");
    }

    @Test
    @DisplayName("Should format date time with single digit day and month with leading zeros")
    void shouldFormatDateTimeWithSingleDigitDayAndMonthWithLeadingZeros() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 5, 8, 7, 6);

        // When
        String formatted = DateFormatter.formatDateTime(dateTime);

        // Then
        assertThat(formatted).isEqualTo("05/03/2024 08:07:06");
        assertThat(formatted).startsWith("05");
        assertThat(formatted).contains("/03/");
        assertThat(formatted).contains("08:07:06");
    }
}
