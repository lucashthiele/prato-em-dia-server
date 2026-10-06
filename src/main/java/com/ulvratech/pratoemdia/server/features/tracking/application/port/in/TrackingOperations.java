package com.ulvratech.pratoemdia.server.features.tracking.application.port.in;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Create;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Event;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Summary;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import java.util.UUID;

public interface TrackingOperations {
    Event create(Create input);

    PageResult<Event> mealEvents(Actor actor, UUID plannedMealId, int page, int size);

    PageResult<Event> patientEvents(Actor actor, int page, int size);

    Summary summary(Actor actor, UUID patientId, UUID planId);
}
