package com.ulvratech.pratoemdia.server.features.identity.application.port.in;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.LogoutInput;

public interface LogoutUser {
  void execute(LogoutInput input);
}
