package com.ulvratech.pratoemdia.server.features.tracking.application.port.in;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.FoodLogTypes.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.FoodLogTypes.FoodLog;
import java.util.UUID;

public interface FoodLogOperations {
    FoodLog get(Actor actor, UUID plannedMealId);
}
