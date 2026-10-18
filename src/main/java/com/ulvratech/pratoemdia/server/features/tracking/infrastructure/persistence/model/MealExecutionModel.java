package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "meal_execution")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealExecutionModel {
    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "execution_date", nullable = false)
    private LocalDate date;

    @Column(name = "meal_time", nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "meal_type", nullable = false)
    private MealType mealType;

    @Column(name = "planned_meal_id")
    private UUID plannedMealId;

    @Column(name = "weekly_plan_id")
    private UUID weeklyPlanId;

    @Column(name = "prescription_version_id", nullable = false)
    private UUID prescriptionVersionId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private PlanningMode mode;

    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "restore_token")
    private UUID restoreToken;

    @Column(name = "restore_expires_at")
    private Instant restoreExpiresAt;

    @OneToMany(mappedBy = "execution", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @OrderBy("order")
    @BatchSize(size = 100)
    @Builder.Default
    private List<MealExecutionItemModel> items = new ArrayList<>();
}
