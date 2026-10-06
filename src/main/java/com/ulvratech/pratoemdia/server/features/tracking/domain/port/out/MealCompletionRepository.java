package com.ulvratech.pratoemdia.server.features.tracking.domain.port.out;

import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealCompletionRepository {
    Optional<MealCompletionEvent> byKey(UUID patientId, String key);

    Optional<UUID> patientForMeal(UUID mealId);

    MealCompletionEvent append(MealCompletionEvent event);

    PageResult<MealCompletionEvent> forMeal(UUID mealId, int page, int size);

    PageResult<MealCompletionEvent> forPatient(UUID patientId, int page, int size);

    List<MealCompletionEvent> forPlan(UUID planId);
}
