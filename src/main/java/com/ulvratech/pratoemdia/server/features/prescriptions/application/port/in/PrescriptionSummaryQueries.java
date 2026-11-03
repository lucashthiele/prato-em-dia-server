package com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.PatientQuery;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionSummary;

public interface PrescriptionSummaryQueries {
    PrescriptionSummary summary(PatientQuery input);
}
