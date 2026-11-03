package com.ulvratech.pratoemdia.server.features.tracking.application.port.in;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Calendar;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.DailySummary;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.DailyMealCategories;
import java.time.LocalDate;
import java.util.UUID;

public interface DiaryOperations {
    Calendar calendar(Actor actor);

    DailySummary dailySummary(Actor actor, LocalDate date);

    DailyMealCategories dailyMealCategories(Actor actor, LocalDate date);

    /** Reads of a linked patient, for the nutritionist {@code actor}. */
    Calendar calendar(Actor actor, UUID patientId);

    DailySummary dailySummary(Actor actor, UUID patientId, LocalDate date);

    DailyMealCategories dailyMealCategories(Actor actor, UUID patientId, LocalDate date);
}
