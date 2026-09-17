package com.ulvratech.pratoemdia.server.features.identity.application.port.in;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangePasswordInput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface ChangePassword extends UseCase<ChangePasswordInput, AuthenticateUserOutput> {}
