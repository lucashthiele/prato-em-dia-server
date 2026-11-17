package com.ulvratech.pratoemdia.server.features.profiles.application.usecase;

import com.ulvratech.pratoemdia.server.features.profiles.application.dto.ProfileOutput;
import com.ulvratech.pratoemdia.server.features.profiles.application.dto.UpdateProfileInput;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.InvalidProfileFieldsException;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.ProfileNotFoundException;
import com.ulvratech.pratoemdia.server.features.profiles.application.port.in.UpdateCurrentProfile;
import com.ulvratech.pratoemdia.server.features.profiles.application.port.out.AccountEmailReader;
import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;
import com.ulvratech.pratoemdia.server.features.profiles.domain.port.out.UserProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class UpdateCurrentProfileUseCase implements UpdateCurrentProfile {
    private final UserProfileRepository repository;
    private final AccountEmailReader emails;

    public UpdateCurrentProfileUseCase(UserProfileRepository repository, AccountEmailReader emails) {
        this.repository = repository;
        this.emails = emails;
    }

    @Override
    public ProfileOutput execute(UpdateProfileInput input) {
        validateFieldsForType(input);
        var profile = repository.findByUserAccountId(input.userAccountId(), input.type())
                .orElseThrow(ProfileNotFoundException::new);

        profile.replaceCommon(input.fullName(), input.phone());
        if (input.type() == ProfileType.NUTRITIONIST) {
            profile.replaceNutritionist(input.registrationNumber(), input.bio());
        } else {
            profile.replacePatient(input.birthDate());
        }

        return ProfileOutput.from(repository.save(profile), emails.email(input.userAccountId()));
    }

    private void validateFieldsForType(UpdateProfileInput input) {
        boolean invalid = input.type() == ProfileType.PATIENT
                ? input.registrationNumber() != null || input.bio() != null
                : input.birthDate() != null;
        if (invalid) {
            throw new InvalidProfileFieldsException();
        }
    }
}
