package com.ulvratech.pratoemdia.server.features.email.application.port.out;

import com.ulvratech.pratoemdia.server.features.email.application.dto.OutgoingEmail;

public interface EmailSender {
    /**
     * Sends synchronously; throws
     * {@link com.ulvratech.pratoemdia.server.features.email.application.exception.EmailDeliveryException}.
     */
    void send(OutgoingEmail email);
}
