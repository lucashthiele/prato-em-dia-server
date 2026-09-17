package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.initialization;

import com.ulvratech.pratoemdia.server.features.identity.application.port.out.UserProfileInitializer;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.profiles.domain.entity.UserProfile;
import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;
import com.ulvratech.pratoemdia.server.features.profiles.domain.port.out.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserProfileInitializerAdapter implements UserProfileInitializer {
    private final UserProfileRepository repository;

    public UserProfileInitializerAdapter(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public void initialize(UUID userAccountId, UserAccountRole role, String fullName) {
        if (role == UserAccountRole.ADMIN) {
            return;
        }
        repository.save(
                UserProfile.create(
                        userAccountId,
                        ProfileType.valueOf(role.name()),
                        fullName
                )
        );
    }
}
