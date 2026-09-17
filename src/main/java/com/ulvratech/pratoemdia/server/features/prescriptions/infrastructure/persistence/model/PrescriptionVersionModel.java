package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "prescription_version")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionVersionModel {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "prescription_id", nullable = false)
  private PrescriptionModel prescription;

  @Column(name = "version_number", nullable = false)
  private int number;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private PrescriptionVersionStatus status;

  @Column(length = 180)
  private String title;

  @Column(columnDefinition = "text")
  private String notes;

  @Column(name = "created_by_user_id", nullable = false)
  private UUID createdByUserId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "archived_at")
  private Instant archivedAt;

  @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  @Builder.Default
  private List<PrescribedMealModel> meals = new ArrayList<>();
}
