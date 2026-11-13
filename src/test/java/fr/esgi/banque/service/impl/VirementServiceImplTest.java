package fr.esgi.banque.service.impl;

import fr.esgi.banque.service.CompteRepository;
import fr.esgi.banque.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class VirementServiceImplTest {

    @Mock
    private CompteRepository repository;

    @Mock
    private NotificationService notifications;

    // Jeudi 12 novembre 2026 : un jour ouvré
    private final Clock horloge = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Europe/Paris"));

    private VirementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VirementServiceImpl(repository, notifications, horloge);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01"})
    void shouldRejectTransferWhenAmountIsNullZeroOrNegative(String montant) {
        // Arrange
        BigDecimal valeur = montant == null ? null : new BigDecimal(montant);

        // Act + Assert
        assertThatThrownBy(() -> service.virer("FR001", "FR002", valeur))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        // Arrange
        BigDecimal montant = new BigDecimal("100");

        // Act + Assert
        assertThatThrownBy(() -> service.virer("FR001", "FR001", montant))
                .isInstanceOf(IllegalArgumentException.class);
    }
}