package com.ulvratech.pratoemdia.server.features.admin.application.port.in;

import java.util.UUID;

public interface ResendNutritionistAccess {
    void execute(UUID actorAccountId, UUID nutritionistAccountId);
}
