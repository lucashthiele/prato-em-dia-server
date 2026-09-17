package com.ulvratech.pratoemdia.server.features.identity.application.port.in;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionPatientAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionPatientAccountOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface ProvisionPatientAccount
        extends UseCase<ProvisionPatientAccountInput, ProvisionPatientAccountOutput> {
}
