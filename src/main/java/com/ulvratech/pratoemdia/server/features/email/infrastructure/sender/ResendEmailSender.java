package com.ulvratech.pratoemdia.server.features.email.infrastructure.sender;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.ulvratech.pratoemdia.server.features.email.application.dto.OutgoingEmail;
import com.ulvratech.pratoemdia.server.features.email.application.exception.EmailDeliveryException;
import com.ulvratech.pratoemdia.server.features.email.application.port.out.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Sends through Resend synchronously and classifies failures for the outbox retry policy. */
@Component
public class ResendEmailSender implements EmailSender {
    private final Resend resend;
    private final String from;

    public ResendEmailSender(Resend resend, @Value("${relationships.mail.from}") String from) {
        this.resend = resend;
        this.from = from;
    }

    @Override
    public void send(OutgoingEmail email) {
        var message = CreateEmailOptions.builder()
                .from(from)
                .to(email.to())
                .subject(email.subject())
                .text(email.body())
                .build();
        try {
            resend.emails().send(message);
        } catch (ResendException error) {
            throw new EmailDeliveryException(describe(error), isTransient(error), error);
        } catch (RuntimeException error) {
            // Unexpected client-side failure (connection reset, parsing): treat like a timeout.
            throw new EmailDeliveryException("Resend call failed: " + error.getClass().getSimpleName(), true, error);
        }
    }

    /** No HTTP status means the call never got an answer (timeout, connection); 5xx and 429 are server-side. */
    static boolean isTransient(ResendException error) {
        var status = error.getStatusCode();
        return status == null || status == 0 || status == 429 || status >= 500;
    }

    private static String describe(ResendException error) {
        var status = error.getStatusCode();
        return (status == null || status == 0 ? "no response" : "HTTP " + status) + ": " + error.getMessage();
    }
}
