package com.ulvratech.pratoemdia.server.features.email.application.port.in;

import com.ulvratech.pratoemdia.server.features.email.application.dto.EnqueuePlanEmailInput;

/** Stores a plan email in the caller's transaction (joins it when one is active). */
public interface EnqueuePlanEmail {
    void execute(EnqueuePlanEmailInput input);
}
