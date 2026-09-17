package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "nutritionist_profile")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NutritionistProfileModel {
  @Id private UUID id;

  @Column(name = "user_account_id", nullable = false, unique = true)
  private UUID userAccountId;

  @Column(name = "full_name", nullable = false, length = 160)
  private String fullName;

  @Column(name = "registration_number", length = 80)
  private String registrationNumber;

  @Column(length = 32)
  private String phone;

  @Column(columnDefinition = "TEXT")
  private String bio;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;
}
