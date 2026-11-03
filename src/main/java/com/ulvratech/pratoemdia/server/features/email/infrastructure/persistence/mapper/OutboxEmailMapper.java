package com.ulvratech.pratoemdia.server.features.email.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.email.domain.entity.OutboxEmail;
import com.ulvratech.pratoemdia.server.features.email.infrastructure.persistence.model.OutboxEmailModel;
import org.springframework.stereotype.Component;

@Component
public class OutboxEmailMapper {
    public OutboxEmail toEntity(OutboxEmailModel m) {
        return new OutboxEmail(
                m.getId(), m.getType(), m.getRecipient(), m.getRecipientName(), m.getInviterName(), m.getAccountId(),
                m.getSubject(), m.getBody(), m.getStatus(), m.getAttempts(), m.getNextAttemptAt(), m.getLastError(),
                m.getCreatedAt(), m.getSentAt()
        );
    }

    public OutboxEmailModel toModel(OutboxEmail e) {
        return OutboxEmailModel.builder()
                .id(e.getId())
                .type(e.getType())
                .recipient(e.getRecipient())
                .recipientName(e.getRecipientName())
                .inviterName(e.getInviterName())
                .accountId(e.getAccountId())
                .subject(e.getSubject())
                .body(e.getBody())
                .status(e.getStatus())
                .attempts(e.getAttempts())
                .nextAttemptAt(e.getNextAttemptAt())
                .lastError(e.getLastError())
                .createdAt(e.getCreatedAt())
                .sentAt(e.getSentAt())
                .build();
    }
}
