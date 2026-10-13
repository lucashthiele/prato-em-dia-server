package com.ulvratech.pratoemdia.server.features.tracking.application.usecase;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.Calendar;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.DailySummary;

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

    private java.util.UUID patient(Actor actor) {
        if (actor == null || !"PATIENT".equals(actor.role()))
            throw new TrackingException(Kind.FORBIDDEN, "Patient access required");
        return data.patientId(actor.accountId())
                .orElseThrow(() -> new TrackingException(Kind.FORBIDDEN, "Patient access required"));
    }
}
