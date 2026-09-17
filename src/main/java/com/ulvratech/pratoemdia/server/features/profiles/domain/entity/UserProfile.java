package com.ulvratech.pratoemdia.server.features.profiles.domain.entity;

import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class UserProfile {
  private final UUID id;
  private final UUID userAccountId;
  private final ProfileType type;
  private final Instant createdAt;
  private final Instant updatedAt;
  private final long version;
  private String fullName;
  private String phone;
  private String registrationNumber;
  private String bio;
  private LocalDate birthDate;

  public UserProfile(
      UUID id,
      UUID userAccountId,
      ProfileType type,
      String fullName,
      String phone,
      String registrationNumber,
      String bio,
      LocalDate birthDate,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this.id = id;
    this.userAccountId = userAccountId;
    this.type = type;
    this.fullName = requireFullName(fullName);
    this.phone = normalize(phone);
    this.registrationNumber = normalize(registrationNumber);
    this.bio = normalize(bio);
    this.birthDate = birthDate;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
    validateRoleSpecificFields();
  }

  public static UserProfile create(UUID userAccountId, ProfileType type, String fullName) {
    return new UserProfile(
        UUID.randomUUID(), userAccountId, type, fullName, null, null, null, null, null, null, 0);
  }

  public void replaceCommon(String fullName, String phone) {
    this.fullName = requireFullName(fullName);
    this.phone = normalize(phone);
  }

  public void replaceNutritionist(String registrationNumber, String bio) {
    requireType(ProfileType.NUTRITIONIST);
    this.registrationNumber = normalize(registrationNumber);
    this.bio = normalize(bio);
  }

  public void replacePatient(LocalDate birthDate) {
    requireType(ProfileType.PATIENT);
    this.birthDate = birthDate;
  }

  private void validateRoleSpecificFields() {
    if (type == ProfileType.PATIENT && (registrationNumber != null || bio != null)) {
      throw new IllegalArgumentException("Patient profile cannot have professional fields");
    }
    if (type == ProfileType.NUTRITIONIST && birthDate != null) {
      throw new IllegalArgumentException("Nutritionist profile cannot have birth date");
    }
  }

  private void requireType(ProfileType expected) {
    if (type != expected) {
      throw new IllegalStateException("Profile operation is not valid for this type");
    }
  }

  private String requireFullName(String value) {
    String normalized = normalize(value);
    if (normalized == null) {
      throw new IllegalArgumentException("Full name is required");
    }
    return normalized;
  }

  private String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserAccountId() {
    return userAccountId;
  }

  public ProfileType getType() {
    return type;
  }

  public String getFullName() {
    return fullName;
  }

  public String getPhone() {
    return phone;
  }

  public String getRegistrationNumber() {
    return registrationNumber;
  }

  public String getBio() {
    return bio;
  }

  public LocalDate getBirthDate() {
    return birthDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }
}
