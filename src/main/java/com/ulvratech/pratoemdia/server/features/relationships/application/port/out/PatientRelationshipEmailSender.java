package com.ulvratech.pratoemdia.server.features.relationships.application.port.out;

public interface PatientRelationshipEmailSender {
    void sendInitialAccess(
            String patientName, String email, String nutritionistName, String initialPassword
    );

    void sendRelationshipNotice(String patientName, String email, String nutritionistName);
}
