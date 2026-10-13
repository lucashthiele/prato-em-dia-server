package com.ulvratech.pratoemdia.server.features.tracking.application.port.out;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExecutionData {
    ExecutionTypes.Execution create(UUID patientId, String key, ExecutionTypes.Write input);

    ExecutionTypes.Execution get(UUID patientId, UUID id);

    List<ExecutionTypes.Execution> list(UUID patientId, LocalDate date);

    ExecutionTypes.Execution replace(
            UUID patientId, UUID id, long expectedVersion,
            String key, ExecutionTypes.Write input
    );

    ExecutionTypes.Execution move(
            UUID patientId, UUID id, long expectedVersion,
            String key, ExecutionTypes.Write destination
    );

    ExecutionTypes.Deleted delete(UUID patientId, UUID id, long expectedVersion, String key);

    ExecutionTypes.Execution restore(UUID patientId, UUID id, UUID token);
}
