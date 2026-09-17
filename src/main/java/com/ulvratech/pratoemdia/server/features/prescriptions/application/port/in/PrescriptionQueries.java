package com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.Actor;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.PatientQuery;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.PrescriptionQuery;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.VersionQuery;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionOutput;
import java.util.List;

public interface PrescriptionQueries {
  List<PrescriptionOutput> list(PatientQuery input);

  PrescriptionOutput get(PrescriptionQuery input);

  List<PrescriptionOutput.VersionOutput> listVersions(PrescriptionQuery input);

  PrescriptionOutput.VersionOutput getVersion(VersionQuery input);

  PrescriptionOutput.VersionOutput current(Actor actor);
}
