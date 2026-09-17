package com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionOutput;

public interface PrescriptionCommands {
  PrescriptionOutput create(Create input);

  PrescriptionOutput.VersionOutput createVersion(PrescriptionQuery input);

  PrescriptionOutput.VersionOutput updateVersion(UpdateVersion input);

  PrescriptionOutput.VersionOutput publish(VersionQuery input);

  PrescriptionOutput.VersionOutput archive(VersionQuery input);

  PrescriptionOutput updatePlanningMode(UpdatePlanningMode input);
}
