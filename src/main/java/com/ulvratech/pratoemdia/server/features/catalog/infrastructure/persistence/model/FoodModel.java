package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.catalog.domain.enums.FoodOrigin;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "food")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodModel {
    @Id
    private UUID id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(length = 180)
    private String brand;

    @Column(length = 100)
    private String category;

    @Column(name = "default_unit", nullable = false, length = 32)
    private String defaultUnit;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "preparation_method", columnDefinition = "text")
    private String preparationMethod;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private FoodOrigin origin;

    @Column(length = 32)
    private String source;

    @Column(name = "source_external_id", length = 64)
    private String sourceExternalId;

    @Column(name = "source_description", length = 255)
    private String sourceDescription;

    @Column(name = "source_version", length = 100)
    private String sourceVersion;

    @Column(name = "normalized_name", length = 180)
    private String normalizedName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;
}
