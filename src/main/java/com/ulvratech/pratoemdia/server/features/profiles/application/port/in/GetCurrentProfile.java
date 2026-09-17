package com.ulvratech.pratoemdia.server.features.profiles.application.port.in;

import com.ulvratech.pratoemdia.server.features.profiles.application.dto.GetProfileInput;
import com.ulvratech.pratoemdia.server.features.profiles.application.dto.ProfileOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface GetCurrentProfile extends UseCase<GetProfileInput, ProfileOutput> {
}
