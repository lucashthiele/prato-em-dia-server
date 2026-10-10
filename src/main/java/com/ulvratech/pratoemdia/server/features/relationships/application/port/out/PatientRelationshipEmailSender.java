package com.ulvratech.pratoemdia.server.features.relationships.application.port.out;

import java.util.concurrent.CompletableFuture;

public interface PatientRelationshipEmailSender {
    CompletableFuture<Void> sendInitialAccess(
            String patientName, String email, String nutritionistName, String initialPassword
    );

    CompletableFuture<Void> sendRelationshipNotice(String patientName, String email, String nutritionistName);
}
