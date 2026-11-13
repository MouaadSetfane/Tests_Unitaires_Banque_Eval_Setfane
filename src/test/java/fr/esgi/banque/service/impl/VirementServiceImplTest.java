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
import fr.esgi.banque.business.Compte;
import fr.esgi.banque.business.CompteIntrouvableException;
import java.util.Optional;
import static org.mockito.Mockito.when;
import fr.esgi.banque.business.Virement;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import fr.esgi.banque.business.SoldeInsuffisantException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

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

    @Test
    void shouldRejectTransferWhenSourceAccountDoesNotExist() {
        // Arrange
        when(repository.trouver("FR001")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.virer("FR001", "FR002", new BigDecimal("100")))
                .isInstanceOf(CompteIntrouvableException.class);
    }

    @Test
    void shouldRejectTransferWhenDestinationAccountDoesNotExist() {
        // Arrange
        Compte source = new Compte("FR001", "Alice", new BigDecimal("1000"), BigDecimal.ZERO);
        when(repository.trouver("FR001")).thenReturn(Optional.of(source));
        when(repository.trouver("FR002")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.virer("FR001", "FR002", new BigDecimal("100")))
                .isInstanceOf(CompteIntrouvableException.class);
    }

    @Test
    void shouldDebitSourceCreditDestinationAndSaveBothAccounts() {
        // Arrange
        Compte source = new Compte("FR001", "Alice", new BigDecimal("1000"), BigDecimal.ZERO);
        Compte destination = new Compte("FR002", "Bob", new BigDecimal("500"), BigDecimal.ZERO);
        when(repository.trouver("FR001")).thenReturn(Optional.of(source));
        when(repository.trouver("FR002")).thenReturn(Optional.of(destination));

        // Act
        service.virer("FR001", "FR002", new BigDecimal("100"));

        // Assert
        assertThat(source.getSolde()).isEqualByComparingTo("900");
        assertThat(destination.getSolde()).isEqualByComparingTo("600");
        verify(repository).sauvegarder(source);
        verify(repository).sauvegarder(destination);
    }

    @Test
    void shouldReturnTransferWithTodayDate() {
        // Arrange
        Compte source = new Compte("FR001", "Alice", new BigDecimal("1000"), BigDecimal.ZERO);
        Compte destination = new Compte("FR002", "Bob", new BigDecimal("500"), BigDecimal.ZERO);
        when(repository.trouver("FR001")).thenReturn(Optional.of(source));
        when(repository.trouver("FR002")).thenReturn(Optional.of(destination));

        // Act
        Virement virement = service.virer("FR001", "FR002", new BigDecimal("100"));

        // Assert
        assertThat(virement.numeroSource()).isEqualTo("FR001");
        assertThat(virement.numeroDestination()).isEqualTo("FR002");
        assertThat(virement.montant()).isEqualByComparingTo("100");
        assertThat(virement.date()).isEqualTo(LocalDate.of(2026, 11, 12));
    }

    @Test
    void shouldPropagateInsufficientBalanceAndSaveNothing() {
        // Arrange
        Compte source = new Compte("FR001", "Alice", new BigDecimal("50"), BigDecimal.ZERO);
        Compte destination = new Compte("FR002", "Bob", new BigDecimal("500"), BigDecimal.ZERO);
        when(repository.trouver("FR001")).thenReturn(Optional.of(source));
        when(repository.trouver("FR002")).thenReturn(Optional.of(destination));

        // Act + Assert
        assertThatThrownBy(() -> service.virer("FR001", "FR002", new BigDecimal("100")))
                .isInstanceOf(SoldeInsuffisantException.class);
        verify(repository, never()).sauvegarder(any());
        assertThat(destination.getSolde()).isEqualByComparingTo("500");
    }

    @Test
    void shouldAlertSourceOwnerWhenAmountExceedsTenThousand() {
        // Arrange
        Compte source = new Compte("FR001", "Alice", new BigDecimal("50000"), BigDecimal.ZERO);
        Compte destination = new Compte("FR002", "Bob", new BigDecimal("500"), BigDecimal.ZERO);
        when(repository.trouver("FR001")).thenReturn(Optional.of(source));
        when(repository.trouver("FR002")).thenReturn(Optional.of(destination));

        // Act
        service.virer("FR001", "FR002", new BigDecimal("10000.01"));

        // Assert
        verify(notifications).alerter(eq("Alice"), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "9999.99", "10000", "10000.00"})
    void shouldNotAlertWhenAmountIsAtMostTenThousand(String montant) {
        // Arrange
        Compte source = new Compte("FR001", "Alice", new BigDecimal("50000"), BigDecimal.ZERO);
        Compte destination = new Compte("FR002", "Bob", new BigDecimal("500"), BigDecimal.ZERO);
        when(repository.trouver("FR001")).thenReturn(Optional.of(source));
        when(repository.trouver("FR002")).thenReturn(Optional.of(destination));

        // Act
        service.virer("FR001", "FR002", new BigDecimal(montant));

        // Assert
        verify(notifications, never()).alerter(any(), any());
    }
}