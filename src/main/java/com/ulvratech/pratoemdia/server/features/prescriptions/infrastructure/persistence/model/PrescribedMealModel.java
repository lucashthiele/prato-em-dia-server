package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescribed_meal")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescribedMealModel {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_version_id", nullable = false) private PrescriptionVersionModel version;
    @Column(nullable = false, length = 120) private String name;
    @Column(name = "meal_order", nullable = false) private int order;
    @Column(name = "suggested_time") private LocalTime suggestedTime;
    @Column(columnDefinition = "text") private String notes;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100) @Builder.Default private List<PrescribedMealItemModel> items = new ArrayList<>();
}
