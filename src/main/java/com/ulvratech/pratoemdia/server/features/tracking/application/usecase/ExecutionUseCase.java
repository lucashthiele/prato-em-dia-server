package com.ulvratech.pratoemdia.server.features.tracking.application.usecase;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Deleted;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Execution;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Write;

import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException.Kind;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.ExecutionOperations;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.DiaryData;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.ExecutionData;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ExecutionUseCase implements ExecutionOperations {
    static final long MAX_RANGE_DAYS = 92;

    private final DiaryData diary;
    private final ExecutionData data;

    public ExecutionUseCase(DiaryData diary, ExecutionData data) {
        this.diary = diary;
        this.data = data;
    }

    private UUID patient(Actor actor) {
        if (actor == null || !"PATIENT".equals(actor.role()))
            throw new TrackingException(Kind.FORBIDDEN, "Patient access required");
        return diary.patientId(actor.accountId())
                .orElseThrow(() -> new TrackingException(Kind.FORBIDDEN, "Patient access required"));
    }

    @Override
    public Execution create(Actor actor, String key, Write input) {
        return data.create(patient(actor), key, input);
    }

    @Override
    public Execution get(Actor actor, UUID id) {
        return data.get(patient(actor), id);
    }

    @Override
    public Execution get(Actor actor, UUID patientId, UUID id) {
        return data.get(linkedPatient(actor, patientId), id);
    }

    @Override
    public List<Execution> list(Actor actor, UUID patientId, LocalDate from, LocalDate to) {
        var patient = linkedPatient(actor, patientId);
        if (from == null || to == null)
            throw new TrackingException(Kind.INVALID, "From and to are required");
        if (to.isBefore(from) || from.plusDays(MAX_RANGE_DAYS).isBefore(to))
            throw new TrackingException(
                    Kind.INVALID, "Period must be ordered and at most " + MAX_RANGE_DAYS + " days"
            );
        return data.list(patient, from, to);
    }

    private UUID linkedPatient(Actor actor, UUID patientId) {
        if (actor == null || !"NUTRITIONIST".equals(actor.role()) || patientId == null
                || !diary.hasActiveRelationship(actor.accountId(), patientId))
            throw new TrackingException(Kind.FORBIDDEN, "Active relationship with the patient required");
        return patientId;
    }

    @Override
    public List<Execution> list(Actor actor, LocalDate date) {
        return data.list(patient(actor), date);
    }

    @Override
    public Execution replace(Actor actor, UUID id, long expectedVersion, String key, Write input) {
        return data.replace(patient(actor), id, expectedVersion, key, input);
    }

    @Override
    public Execution move(
            Actor actor, UUID id, long expectedVersion,
            String key, Write destination
    ) {
        return data.move(patient(actor), id, expectedVersion, key, destination);
    }

    @Override
    public Deleted delete(Actor actor, UUID id, long expectedVersion, String key) {
        return data.delete(patient(actor), id, expectedVersion, key);
    }

    @Override
    public Execution restore(Actor actor, UUID id, UUID token) {
        return data.restore(patient(actor), id, token);
    }
}
