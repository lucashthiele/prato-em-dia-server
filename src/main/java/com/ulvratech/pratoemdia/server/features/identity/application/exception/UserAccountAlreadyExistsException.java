package com.ulvratech.pratoemdia.server.features.identity.application.exception;

public class UserAccountAlreadyExistsException extends RuntimeException {
    public UserAccountAlreadyExistsException(String email) {
        super("Usuário já existe com email " + email);
    }
}
