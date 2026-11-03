package com.ulvratech.pratoemdia.server.features.planning.application.port.out;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanGeneratedNotice;

public interface PlanGeneratedNotifier {
    void planGenerated(PlanGeneratedNotice notice);

    /**
     * Scheduled generation was rejected by a prescription rule; it is not retried, the patient is told to contact the
     * nutritionist.
     */
    void planGenerationRejected(java.util.UUID patientId);
}
