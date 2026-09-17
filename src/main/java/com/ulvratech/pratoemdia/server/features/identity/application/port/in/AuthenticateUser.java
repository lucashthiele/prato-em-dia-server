package com.ulvratech.pratoemdia.server.features.identity.application.port.in;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface AuthenticateUser extends UseCase<AuthenticateUserInput, AuthenticateUserOutput> {
}
