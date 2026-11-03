package com.ulvratech.pratoemdia.server.features.email.application.port.in;

import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueueAccessEmailInput;

import java.util.UUID;

/** Stores an access email reference in the caller's transaction and returns the outbox id. */
public interface EnqueueAccessEmail {
    UUID execute(EnqueueAccessEmailInput input);
}
