package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.email;

import com.resend.Resend;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResendNotificationEmailSenderTest {
    @Test
    void schedulesResendCallWithoutWaitingForIt() {
        var tasks = new ArrayList<Runnable>();
        Executor executor = tasks::add;
        var sender = new ResendNotificationEmailSender(new Resend("test-key"), executor, "onboarding@resend.dev");

        var result = sender.send("patient@example.com", "Subject", "Body");

        assertEquals(1, tasks.size());
        assertFalse(result.isDone());
    }
}
