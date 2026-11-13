package fr.esgi.banque.business;

public class CompteIntrouvableException extends RuntimeException {

    public CompteIntrouvableException(String numero) {
        super("Compte introuvable : " + numero);
    }
}