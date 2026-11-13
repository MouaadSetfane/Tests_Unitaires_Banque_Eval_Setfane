package fr.esgi.banque.business;

public class VirementRefuseException extends RuntimeException {

    public VirementRefuseException(String message) {
        super(message);
    }
}