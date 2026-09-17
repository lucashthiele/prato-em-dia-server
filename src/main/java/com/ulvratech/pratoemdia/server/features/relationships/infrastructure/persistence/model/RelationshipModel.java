package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "nutritionist_patient_relationship")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipModel {
  @Id private UUID id;

  @Column(name = "nutritionist_id", nullable = false)
  private UUID nutritionistId;

  @Column(name = "patient_id", nullable = false)
  private UUID patientId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private RelationshipStatus status;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "ended_by_user_account_id")
  private UUID endedByUserAccountId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "email_delivery_status", nullable = false)
  private RelationshipEmailStatus emailDeliveryStatus;

  @Column(name = "email_attempt_count", nullable = false)
  private int emailAttemptCount;

  @Column(name = "email_last_attempt_at")
  private Instant emailLastAttemptAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;
}
