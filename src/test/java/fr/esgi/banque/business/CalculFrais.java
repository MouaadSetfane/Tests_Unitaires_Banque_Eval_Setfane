package fr.esgi.banque.business;

import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class CalculFraisTest {

    private final CalculFrais calculFrais = new CalculFrais();

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 17})
    void shouldRejectWhenTitulaireIsMinor(int age) {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> calculFrais.fraisMensuels(age, new BigDecimal("500"), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptWhenTitulaireIsExactlyMajor() {
        // Arrange / Act
        BigDecimal frais = calculFrais.fraisMensuels(18, new BigDecimal("500"), false);

        // Assert
        assertThat(frais).isNotNull();
    }

    @Test
    void shouldRejectWhenSoldeMoyenIsNull() {
        // Arrange / Act / Assert
        assertThatThrownBy(() -> calculFrais.fraisMensuels(30, null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldChargeTwoEurosForAccountKeeping() {
        // Arrange / Act
        BigDecimal frais = calculFrais.fraisMensuels(30, new BigDecimal("500"), false);

        // Assert
        assertThat(frais).isEqualByComparingTo("2.00");
    }

    @ParameterizedTest
    @CsvSource({
            "18, 0.00",
            "25, 0.00",
            "26, 2.00",
            "27, 2.00"
    })
    void shouldWaiveKeepingFeesOnlyBelowAge26(int age, String fraisAttendus) {
        // Arrange
        BigDecimal soldeMoyen = new BigDecimal("500");

        // Act
        BigDecimal frais = calculFrais.fraisMensuels(age, soldeMoyen, false);

        // Assert
        assertThat(frais).isEqualByComparingTo(fraisAttendus);
    }
}