package fr.esgi.banque.business;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test d'exemple fourni pour vérifier que le projet est bien configuré.
 * À compléter (partie A).
 */
class CompteTest {

	@Test
	void shouldIncreaseBalanceWhenCrediting() {
		// Arrange
		Compte compte = new Compte("FR001", "Alice", new BigDecimal("100.00"), new BigDecimal("200"));

		// Act
		compte.crediter(new BigDecimal("50.00"));

		// Assert
		assertThat(compte.getSolde()).isEqualByComparingTo("150.00");
	}
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  "})
	void shouldRejectCreationWhenNumeroIsMissing(String numero) {
		// Arrange / Act / Assert
		assertThatThrownBy(() -> new Compte(numero, "Alice", new BigDecimal("100"), new BigDecimal("200")))
				.isInstanceOf(IllegalArgumentException.class);
	}
	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  "})
	void shouldRejectCreationWhenTitulaireIsMissing(String titulaire) {
		// Arrange / Act / Assert
		assertThatThrownBy(() -> new Compte("FR001", titulaire, new BigDecimal("100"), new BigDecimal("200")))
				.isInstanceOf(IllegalArgumentException.class);
	}
	@Test
	void shouldRejectCreationWhenSoldeInitialIsNull() {
		// Arrange / Act / Assert
		assertThatThrownBy(() -> new Compte("FR001", "Alice", null, new BigDecimal("200")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectCreationWhenDecouvertIsNull() {
		// Arrange / Act / Assert
		assertThatThrownBy(() -> new Compte("FR001", "Alice", new BigDecimal("100"), null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectCreationWhenDecouvertIsNegative() {
		// Arrange / Act / Assert
		assertThatThrownBy(() -> new Compte("FR001", "Alice", new BigDecimal("100"), new BigDecimal("-1")))
				.isInstanceOf(IllegalArgumentException.class);
	}
	@ParameterizedTest
	@CsvSource({"0", "0.00", "-1", "-0.01"})
	void shouldRejectCreditWhenAmountIsNotStrictlyPositive(String montant) {
		// Arrange
		Compte compte = new Compte("FR001", "Alice", new BigDecimal("100.00"), new BigDecimal("200"));

		// Act + Assert
		assertThatThrownBy(() -> compte.crediter(new BigDecimal(montant)))
				.isInstanceOf(IllegalArgumentException.class);
	}
	@ParameterizedTest
	@CsvSource({"0", "0.00", "-1", "-0.01"})
	void shouldRejectDebitWhenAmountIsNotStrictlyPositive(String montant) {
		// Arrange
		Compte compte = new Compte("FR001", "Alice", new BigDecimal("100.00"), new BigDecimal("200"));

		// Act + Assert
		assertThatThrownBy(() -> compte.debiter(new BigDecimal(montant)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
