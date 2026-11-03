package com.ulvratech.pratoemdia.server.features.email.application.dto;

import com.ulvratech.pratoemdia.server.features.email.domain.enums.EmailType;

public record EnqueuePlanEmailInput(EmailType type, String recipient, String subject, String body) {
}
