package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.email;

import com.resend.Resend;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ResendPatientRelationshipEmailSenderTest {
    @Test
    void schedulesResendCallWithoutWaitingForIt() {
        var tasks = new ArrayList<Runnable>();
        Executor executor = tasks::add;
        var sender = new ResendPatientRelationshipEmailSender(
                new Resend("test-key"), executor, "onboarding@resend.dev", "http://localhost:3000"
        );

        var result = sender.sendRelationshipNotice("Patient", "patient@example.com", "Nutritionist");

        assertEquals(1, tasks.size());
        assertFalse(result.isDone());
    }
}
