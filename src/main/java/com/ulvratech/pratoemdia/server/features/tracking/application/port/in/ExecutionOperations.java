package com.ulvratech.pratoemdia.server.features.tracking.application.port.in;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Deleted;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Execution;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Write;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExecutionOperations {
    Execution create(Actor actor, String key, Write input);

    Execution get(Actor actor, UUID id);

    /** Read of a linked patient's execution, for the nutritionist {@code actor}. */
    Execution get(Actor actor, UUID patientId, UUID id);

    /** Active executions of a linked patient between {@code from} and {@code to}, inclusive. */
    List<Execution> list(Actor actor, UUID patientId, LocalDate from, LocalDate to);

    List<Execution> list(Actor actor, LocalDate date);

    Execution replace(Actor actor, UUID id, long expectedVersion, String key, Write input);

    Execution move(Actor actor, UUID id, long expectedVersion, String key, Write destination);

    Deleted delete(Actor actor, UUID id, long expectedVersion, String key);

    Execution restore(Actor actor, UUID id, UUID token);
}
