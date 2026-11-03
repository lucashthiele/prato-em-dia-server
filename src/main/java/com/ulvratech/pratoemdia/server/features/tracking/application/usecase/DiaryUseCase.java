package com.ulvratech.pratoemdia.server.features.tracking.application.usecase;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Calendar;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.DailyMealCategories;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.DailySummary;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.MealCategory;
import com.ulvratech.pratoemdia.server.features.tracking.domain.MealCategories;

import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException.Kind;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.DiaryOperations;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.DiaryData;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class DiaryUseCase implements DiaryOperations {
    private final DiaryData data;

    public DiaryUseCase(DiaryData data) {
        this.data = data;
    }

    @Override
    public Calendar calendar(Actor actor) {
        return data.calendar(patient(actor));
    }

    @Override
    public DailySummary dailySummary(Actor actor, LocalDate date) {
        if (date == null)
            throw new TrackingException(Kind.INVALID, "Date is required");
        return data.dailySummary(patient(actor), date);
    }

    @Override
    public DailyMealCategories dailyMealCategories(Actor actor, LocalDate date) {
        if (date == null)
            throw new TrackingException(Kind.INVALID, "Date is required");
        var statuses = MealCategories.derive(data.dayFacts(patient(actor), date));
        return new DailyMealCategories(
                date,
                statuses.stream().map(
                        status -> new MealCategory(
                                status.mealType(), status.state(), status.canRegister(),
                                status.blockedReason(), status.plannedMealId(),
                                status.coveredItemIds().size(), status.uncoveredItemIds().size(),
                                status.coveredItemIds(), status.uncoveredItemIds()
                        )
                ).toList()
        );
    }

    private java.util.UUID patient(Actor actor) {
        if (actor == null || !"PATIENT".equals(actor.role()))
            throw new TrackingException(Kind.FORBIDDEN, "Patient access required");
        return data.patientId(actor.accountId())
                .orElseThrow(() -> new TrackingException(Kind.FORBIDDEN, "Patient access required"));
    }
}
