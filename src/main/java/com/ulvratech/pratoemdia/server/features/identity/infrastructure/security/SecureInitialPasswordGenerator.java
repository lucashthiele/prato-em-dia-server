package com.ulvratech.pratoemdia.server.features.identity.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.identity.application.port.out.InitialPasswordGenerator;
import java.security.SecureRandom;

public class SecureInitialPasswordGenerator implements InitialPasswordGenerator {
  private static final char[] ALPHABET =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
  private final SecureRandom random;

  public SecureInitialPasswordGenerator(SecureRandom random) {
    this.random = random;
  }

  public String generate() {
    var result = new char[20];
    for (int i = 0; i < result.length; i++) result[i] = ALPHABET[random.nextInt(ALPHABET.length)];
    return new String(result);
  }
}
