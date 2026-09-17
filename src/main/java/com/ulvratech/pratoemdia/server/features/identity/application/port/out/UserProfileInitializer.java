package com.ulvratech.pratoemdia.server.features.identity.application.port.out;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import java.util.UUID;

public interface UserProfileInitializer {
  void initialize(UUID userAccountId, UserAccountRole role, String fullName);
}
