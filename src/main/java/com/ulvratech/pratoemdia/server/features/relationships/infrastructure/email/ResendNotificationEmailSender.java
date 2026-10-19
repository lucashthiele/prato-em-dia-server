package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.NotificationEmailSender;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class ResendNotificationEmailSender implements NotificationEmailSender {
    private final Resend resend;
    private final Executor executor;
    private final String from;

    public ResendNotificationEmailSender(
            Resend resend,
            @Qualifier("notificationEmailExecutor") Executor executor,
            @Value("${relationships.mail.from}") String from
    ) {
        this.resend = resend;
        this.executor = executor;
        this.from = from;
    }

    @Override
    public CompletableFuture<Void> send(String to, String subject, String body) {
        var message = CreateEmailOptions.builder().from(from).to(to).subject(subject).text(body).build();
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        resend.emails().send(message);
                    } catch (ResendException e) {
                        throw new IllegalStateException("Resend rejected notification email", e);
                    }
                },
                executor
        );
    }
}
