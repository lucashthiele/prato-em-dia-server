package com.ulvratech.pratoemdia.server.features.email.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailStatus;
import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType;
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
@Table(name = "email_outbox")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEmailModel {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EmailType type;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "recipient_name", length = 160)
    private String recipientName;

    @Column(name = "inviter_name", length = 160)
    private String inviterName;

    @Column(name = "account_id")
    private UUID accountId;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EmailStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;
}
