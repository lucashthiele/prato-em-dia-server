package com.ulvratech.pratoemdia.server.features.consent.application.port.in;

import com.ulvratech.pratoemdia.server.features.consent.application.dto.ConsentStatusOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

import java.util.UUID;

public interface GetConsentStatus extends UseCase<UUID, ConsentStatusOutput> {
}
