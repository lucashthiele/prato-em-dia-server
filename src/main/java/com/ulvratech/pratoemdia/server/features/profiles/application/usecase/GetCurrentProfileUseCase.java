package com.ulvratech.pratoemdia.server.features.profiles.application.usecase;

import com.ulvratech.pratoemdia.server.features.profiles.application.dto.GetProfileInput;
import com.ulvratech.pratoemdia.server.features.profiles.application.dto.ProfileOutput;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.ProfileNotFoundException;
import com.ulvratech.pratoemdia.server.features.profiles.application.port.in.GetCurrentProfile;
import com.ulvratech.pratoemdia.server.features.profiles.domain.port.out.UserProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentProfileUseCase implements GetCurrentProfile {
    private final UserProfileRepository repository;

    public GetCurrentProfileUseCase(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProfileOutput execute(GetProfileInput input) {
        return repository.findByUserAccountId(input.userAccountId(), input.type())
                .map(ProfileOutput::from)
                .orElseThrow(ProfileNotFoundException::new);
    }
}
