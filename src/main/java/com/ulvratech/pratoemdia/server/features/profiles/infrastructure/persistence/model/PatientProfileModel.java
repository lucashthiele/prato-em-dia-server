package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "patient_profile")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PatientProfileModel {
  @Id private UUID id;

  @Column(name = "user_account_id", nullable = false, unique = true)
  private UUID userAccountId;

  @Column(name = "full_name", nullable = false, length = 160)
  private String fullName;

  @Column(length = 32)
  private String phone;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;
}
