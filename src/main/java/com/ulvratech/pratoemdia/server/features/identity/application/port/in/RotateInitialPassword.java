package com.ulvratech.pratoemdia.server.features.identity.application.port.in;

import java.util.UUID;

public interface RotateInitialPassword {
  String execute(UUID userAccountId);
}
