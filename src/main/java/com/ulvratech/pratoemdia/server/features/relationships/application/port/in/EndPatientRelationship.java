package com.ulvratech.pratoemdia.server.features.relationships.application.port.in;

import java.util.UUID;

public interface EndPatientRelationship {
  void execute(UUID patientAccountId);
}
