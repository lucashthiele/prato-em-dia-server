package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.profiles.domain.entity.UserProfile;
import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model.NutritionistProfileModel;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model.PatientProfileModel;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {
    public UserProfile toDomain(NutritionistProfileModel model) {
        return new UserProfile(
            model.getId(), model.getUserAccountId(), ProfileType.NUTRITIONIST,
            model.getFullName(), model.getPhone(), model.getRegistrationNumber(),
            model.getBio(), null, model.getCreatedAt(), model.getUpdatedAt(),
            model.getVersion()
        );
    }

    public UserProfile toDomain(PatientProfileModel model) {
        return new UserProfile(
            model.getId(), model.getUserAccountId(), ProfileType.PATIENT,
            model.getFullName(), model.getPhone(), null, null, model.getBirthDate(),
            model.getCreatedAt(), model.getUpdatedAt(), model.getVersion()
        );
    }

    public NutritionistProfileModel toNutritionistModel(UserProfile profile) {
        return NutritionistProfileModel.builder()
            .id(profile.getId())
            .userAccountId(profile.getUserAccountId())
            .fullName(profile.getFullName())
            .phone(profile.getPhone())
            .registrationNumber(profile.getRegistrationNumber())
            .bio(profile.getBio())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .version(profile.getVersion())
            .build();
    }

    public PatientProfileModel toPatientModel(UserProfile profile) {
        return PatientProfileModel.builder()
            .id(profile.getId())
            .userAccountId(profile.getUserAccountId())
            .fullName(profile.getFullName())
            .phone(profile.getPhone())
            .birthDate(profile.getBirthDate())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .version(profile.getVersion())
            .build();
    }
}
