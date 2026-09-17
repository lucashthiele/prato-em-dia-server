package com.ulvratech.pratoemdia.server.features.profiles.application.port.in;

import com.ulvratech.pratoemdia.server.features.profiles.application.dto.ProfileOutput;
import com.ulvratech.pratoemdia.server.features.profiles.application.dto.UpdateProfileInput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface UpdateCurrentProfile extends UseCase<UpdateProfileInput, ProfileOutput> {}
