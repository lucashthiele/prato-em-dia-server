package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.profiles.domain.entity.UserProfile;
import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;
import com.ulvratech.pratoemdia.server.features.profiles.domain.port.out.UserProfileRepository;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.mapper.UserProfileMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserProfileRepositoryAdapter implements UserProfileRepository {
  private final NutritionistProfileJpaRepository nutritionists;
  private final PatientProfileJpaRepository patients;
  private final UserProfileMapper mapper;

  public UserProfileRepositoryAdapter(
      NutritionistProfileJpaRepository nutritionists,
      PatientProfileJpaRepository patients,
      UserProfileMapper mapper) {
    this.nutritionists = nutritionists;
    this.patients = patients;
    this.mapper = mapper;
  }

  @Override
  public UserProfile save(UserProfile profile) {
    if (profile.getType() == ProfileType.NUTRITIONIST) {
      return mapper.toDomain(nutritionists.save(mapper.toNutritionistModel(profile)));
    }
    return mapper.toDomain(patients.save(mapper.toPatientModel(profile)));
  }

  @Override
  public Optional<UserProfile> findByUserAccountId(UUID userAccountId, ProfileType type) {
    if (type == ProfileType.NUTRITIONIST) {
      return nutritionists.findByUserAccountId(userAccountId).map(mapper::toDomain);
    }
    return patients.findByUserAccountId(userAccountId).map(mapper::toDomain);
  }
}
