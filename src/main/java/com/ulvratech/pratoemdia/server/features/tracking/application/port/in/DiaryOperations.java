package com.ulvratech.pratoemdia.server.features.tracking.application.port.in;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Calendar;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.DailySummary;
import java.time.LocalDate;

public interface DiaryOperations {
    Calendar calendar(Actor actor);

    DailySummary dailySummary(Actor actor, LocalDate date);
}
