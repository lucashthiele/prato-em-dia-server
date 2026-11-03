package com.ulvratech.pratoemdia.server.features.identity.application.port.out;

public interface LoginAttemptLimiter {
    /**
     * Throws
     * {@link com.ulvratech.pratoemdia.server.features.identity.application.exception.TooManyLoginAttemptsException}
     * when e-mail or IP is blocked.
     */
    void checkAllowed(String email, String ip);

    void recordFailure(String email, String ip);

    /** A successful login forgets the e-mail's failures (the IP's are kept). */
    void recordSuccess(String email);
}
