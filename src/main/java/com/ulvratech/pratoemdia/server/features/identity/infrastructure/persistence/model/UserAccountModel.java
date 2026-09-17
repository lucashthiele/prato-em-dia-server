package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.enums.UserAccountModelRole;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.enums.UserAccountModelStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_account")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAccountModel {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "email")
  private String email;

  @Column(name = "password_hash")
  private String passwordHash;

  @Column(name = "password_change_required", nullable = false)
  private boolean passwordChangeRequired;

  @Enumerated(EnumType.STRING)
  @Column(name = "role")
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private UserAccountModelRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  private UserAccountModelStatus status;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "version")
  @Version
  private int version;
}
