package com.ulvratech.pratoemdia.server.features.identity.application.port.out;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.GeneratedRefreshToken;

public interface RefreshTokenGenerator {
  GeneratedRefreshToken generate();
}
