package com.ulvratech.pratoemdia.server.features.identity.application.port.out;

public interface RefreshTokenHasher {
    String hash(String token);
}
