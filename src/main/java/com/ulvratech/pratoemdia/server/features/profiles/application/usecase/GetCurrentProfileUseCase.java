package com.ulvratech.pratoemdia.server.features.profiles.application.usecase;

import com.ulvratech.pratoemdia.server.features.profiles.application.dto.GetProfileInput;
import com.ulvratech.pratoemdia.server.features.profiles.application.dto.ProfileOutput;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.ProfileNotFoundException;
import com.ulvratech.pratoemdia.server.features.profiles.application.port.in.GetCurrentProfile;
import com.ulvratech.pratoemdia.server.features.profiles.application.port.out.AccountEmailReader;
import com.ulvratech.pratoemdia.server.features.profiles.domain.port.out.UserProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class GetCurrentProfileUseCase implements GetCurrentProfile {
    private final UserProfileRepository repository;
    private final AccountEmailReader emails;

    public GetCurrentProfileUseCase(UserProfileRepository repository, AccountEmailReader emails) {
        this.repository = repository;
        this.emails = emails;
    }

    @Override
    public ProfileOutput execute(GetProfileInput input) {
        var email = emails.email(input.userAccountId());
        if (input.type() == null) {
            return ProfileOutput.admin(input.userAccountId(), email);
        }
        return repository.findByUserAccountId(input.userAccountId(), input.type())
                .map(profile -> ProfileOutput.from(profile, email))
                .orElseThrow(ProfileNotFoundException::new);
    }
}
