package com.ulvratech.pratoemdia.server.features.identity.application.port.out;

public interface PasswordHasher {
  String hash(String rawPassword);

  boolean matches(String rawPassword, String passwordHash);
}
