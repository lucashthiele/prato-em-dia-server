package com.ulvratech.pratoemdia.server.features.tracking.application.port.out;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Calendar;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.DailySummary;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DiaryData {
    Optional<UUID> patientId(UUID accountId);

    Calendar calendar(UUID patientId);

    DailySummary dailySummary(UUID patientId, LocalDate date);
}
