package com.ulvratech.pratoemdia.server.features.profiles.domain.port.out;

import com.ulvratech.pratoemdia.server.features.profiles.domain.entity.UserProfile;
import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {
    UserProfile save(UserProfile profile);

    Optional<UserProfile> findByUserAccountId(UUID userAccountId, ProfileType type);
}
