package com.ulvratech.pratoemdia.server.features.email.infrastructure.scheduling;

import com.ulvratech.pratoemdia.server.features.email.application.port.in.DispatchDueEmails;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the outbox once a minute. */
@Component
public class EmailOutboxScheduler {
    private final DispatchDueEmails dispatchDueEmails;

    public EmailOutboxScheduler(DispatchDueEmails dispatchDueEmails) {
        this.dispatchDueEmails = dispatchDueEmails;
    }

    @Scheduled(cron = "0 * * * * *")
    public void dispatch() {
        dispatchDueEmails.execute();
    }
}
