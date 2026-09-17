package com.ulvratech.pratoemdia.server.features.identity.application.port.in;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.CreateUserAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.CreateUserAccountOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface CreateUserAccount extends UseCase<CreateUserAccountInput, CreateUserAccountOutput> {
}
