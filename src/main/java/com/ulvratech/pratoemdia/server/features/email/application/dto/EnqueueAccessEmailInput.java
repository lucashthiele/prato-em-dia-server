package com.ulvratech.pratoemdia.server.features.email.application.dto;

import java.util.UUID;

/** Reference to the account that must receive access; {@code inviterName} may be null (administration). */
public record EnqueueAccessEmailInput(UUID accountId, String recipient, String recipientName, String inviterName) {
}
