package com.ulvratech.pratoemdia.server.features.email.domain.enums;

public enum EmailStatus {
    PENDING, SENT,
    /** Permanent failure (for example a 4xx from the provider): never retried. */
    FAILED,
    /** Server-side failures exhausted every attempt: the critical incident. */
    DEAD;

    public boolean isFinal() {
        return this != PENDING;
    }
}
