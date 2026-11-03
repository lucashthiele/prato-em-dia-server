package com.ulvratech.pratoemdia.server.features.consent.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consent_record")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConsentRecordModel {
    @Id
    private UUID id;

    @Column(name = "user_account_id", nullable = false)
    private UUID userAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private ConsentDocumentType documentType;

    @Column(nullable = false, length = 40)
    private String version;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;
}
