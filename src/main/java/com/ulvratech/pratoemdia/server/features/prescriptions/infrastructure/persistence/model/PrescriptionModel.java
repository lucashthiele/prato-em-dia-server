package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescription")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescriptionModel {
    @Id private UUID id;
    @Column(name = "patient_id", nullable = false) private UUID patientId;
    @Column(name = "nutritionist_id", nullable = false) private UUID nutritionistId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false) private PrescriptionStatus status;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20) @Builder.Default
    private List<PrescriptionVersionModel> versions = new ArrayList<>();
}
