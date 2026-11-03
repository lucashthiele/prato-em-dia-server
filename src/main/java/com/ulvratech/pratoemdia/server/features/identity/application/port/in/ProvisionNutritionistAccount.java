package com.ulvratech.pratoemdia.server.features.identity.application.port.in;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionNutritionistAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionNutritionistAccountOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

/**
 * Creates an active nutritionist account that must change its password. The account starts with a
 * throwaway password nobody knows; the real temporary password is generated when the access email
 * is sent (email outbox).
 */
public interface ProvisionNutritionistAccount
        extends UseCase<ProvisionNutritionistAccountInput, ProvisionNutritionistAccountOutput> {
}
