package com.ulvratech.pratoemdia.server.features.consent.application.port.in;

import com.ulvratech.pratoemdia.server.features.consent.application.dto.AcceptConsentInput;

public interface AcceptConsent {
    void execute(AcceptConsentInput input);
}
